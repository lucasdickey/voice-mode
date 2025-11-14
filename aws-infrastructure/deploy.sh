#!/bin/bash

# Voice Mode AWS Infrastructure Deployment Script
# Prerequisites:
# - AWS CLI configured with credentials
# - Python 3.9+
# - Docker (for Lambda layer packaging)
# - OpenAI API key

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
ENVIRONMENT=${1:-dev}
STACK_NAME="voice-mode-stack-${ENVIRONMENT}"
AWS_REGION=${AWS_REGION:-us-east-1}

echo -e "${YELLOW}=== Voice Mode AWS Deployment ===${NC}"
echo "Environment: $ENVIRONMENT"
echo "Stack Name: $STACK_NAME"
echo "Region: $AWS_REGION"

# Validate parameters
if [ -z "$OPENAI_API_KEY" ]; then
  echo -e "${RED}Error: OPENAI_API_KEY environment variable not set${NC}"
  exit 1
fi

if [ -z "$VOICE_MODE_API_KEY" ]; then
  echo -e "${RED}Error: VOICE_MODE_API_KEY environment variable not set${NC}"
  exit 1
fi

# Step 1: Build Lambda function
echo -e "${YELLOW}Step 1: Building Lambda function...${NC}"
cd aws-lambda/transcribe
npm install
npm run build
cd - > /dev/null

# Step 2: Create Lambda layer for dependencies
echo -e "${YELLOW}Step 2: Creating Lambda layer...${NC}"
mkdir -p lambda-layer/nodejs
cd lambda-layer/nodejs
npm init -y > /dev/null
npm install --production \
  aws-sdk \
  uuid \
  form-data
cd - > /dev/null
cd lambda-layer
zip -r ../aws-infrastructure/lambda-layer.zip . > /dev/null
cd - > /dev/null

# Step 3: Validate CloudFormation template
echo -e "${YELLOW}Step 3: Validating CloudFormation template...${NC}"
aws cloudformation validate-template \
  --template-body file://aws-infrastructure/cloudformation.yaml \
  --region $AWS_REGION > /dev/null
echo -e "${GREEN}Template is valid${NC}"

# Step 4: Deploy or update CloudFormation stack
echo -e "${YELLOW}Step 4: Deploying CloudFormation stack...${NC}"

STACK_EXISTS=$(aws cloudformation describe-stacks \
  --stack-name $STACK_NAME \
  --region $AWS_REGION 2>/dev/null | grep -q "StackName" && echo "true" || echo "false")

if [ "$STACK_EXISTS" = "true" ]; then
  echo "Updating existing stack..."
  aws cloudformation update-stack \
    --stack-name $STACK_NAME \
    --template-body file://aws-infrastructure/cloudformation.yaml \
    --parameters \
      ParameterKey=Environment,ParameterValue=$ENVIRONMENT \
      ParameterKey=OpenAIApiKey,ParameterValue=$OPENAI_API_KEY \
      ParameterKey=ApiKeyValue,ParameterValue=$VOICE_MODE_API_KEY \
    --capabilities CAPABILITY_NAMED_IAM \
    --region $AWS_REGION
else
  echo "Creating new stack..."
  aws cloudformation create-stack \
    --stack-name $STACK_NAME \
    --template-body file://aws-infrastructure/cloudformation.yaml \
    --parameters \
      ParameterKey=Environment,ParameterValue=$ENVIRONMENT \
      ParameterKey=OpenAIApiKey,ParameterValue=$OPENAI_API_KEY \
      ParameterKey=ApiKeyValue,ParameterValue=$VOICE_MODE_API_KEY \
    --capabilities CAPABILITY_NAMED_IAM \
    --region $AWS_REGION
fi

echo -e "${YELLOW}Waiting for stack to complete...${NC}"
aws cloudformation wait stack-create-complete \
  --stack-name $STACK_NAME \
  --region $AWS_REGION 2>/dev/null || \
aws cloudformation wait stack-update-complete \
  --stack-name $STACK_NAME \
  --region $AWS_REGION 2>/dev/null || true

# Step 5: Upload Lambda function code
echo -e "${YELLOW}Step 5: Uploading Lambda function code...${NC}"
FUNCTION_NAME=$(aws cloudformation describe-stacks \
  --stack-name $STACK_NAME \
  --region $AWS_REGION \
  --query "Stacks[0].Outputs[?OutputKey=='TranscribeFunctionName'].OutputValue" \
  --output text)

if [ -n "$FUNCTION_NAME" ]; then
  aws lambda update-function-code \
    --function-name $FUNCTION_NAME \
    --zip-file fileb://aws-lambda/transcribe/function.zip \
    --region $AWS_REGION > /dev/null
  echo -e "${GREEN}Lambda function updated: $FUNCTION_NAME${NC}"
fi

# Step 6: Retrieve outputs
echo -e "${YELLOW}Step 6: Retrieving stack outputs...${NC}"
echo ""
echo -e "${GREEN}=== Deployment Complete ===${NC}"
echo ""

aws cloudformation describe-stacks \
  --stack-name $STACK_NAME \
  --region $AWS_REGION \
  --query "Stacks[0].Outputs" \
  --output table

echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Update Android app with API endpoint from above"
echo "2. Test with: curl -X POST https://YOUR-API-ENDPOINT/transcribe \\"
echo "     -H 'Authorization: Bearer $VOICE_MODE_API_KEY' \\"
echo "     -d '{\"audio\":\"base64-audio\",\"filename\":\"test.m4a\"}'"
echo ""
