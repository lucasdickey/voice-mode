# Voice Mode - Command Reference Guide

Quick reference for common commands across the project.

## Project Setup

### Clone & Initialize
```bash
git clone https://github.com/lucasdickey/voice-mode.git
cd voice-mode
git pull origin main
```

### View Project Status
```bash
git log --oneline -10
git status
git branch -a
```

## Android Build Commands

### Build Debug APK
```bash
./gradlew clean assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk (8.9 MB)
```

### Build Release APK
```bash
./gradlew clean assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Install on Device
```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Install with replacement
adb install -r app/build/outputs/apk/debug/app-debug.apk

# List connected devices
adb devices

# Install on specific device
adb -s DEVICE_ID install app/build/outputs/apk/debug/app-debug.apk
```

### View Logs
```bash
# View all logs
adb logcat

# Filter for Voice Mode logs
adb logcat | grep VoiceMode

# Filter for errors
adb logcat | grep Error

# Clear logs
adb logcat -c

# Save logs to file
adb logcat > logfile.txt
```

### Check App Info
```bash
adb shell pm list packages | grep voicemode
adb shell dumpsys package com.voicemode
```

## AWS Credentials & Configuration

### Configure AWS CLI
```bash
aws configure
# Enter: Access Key ID, Secret Access Key, Region, Output format
```

### Set Environment Variables
```bash
export AWS_REGION="us-east-1"
export OPENAI_API_KEY="sk-..."
export VOICE_MODE_API_KEY="a1b2c3d4..."

# Verify
echo $OPENAI_API_KEY
echo $VOICE_MODE_API_KEY
```

### Generate API Key
```bash
openssl rand -hex 32
# Output: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
```

## AWS Infrastructure Commands

### Deploy Infrastructure
```bash
# Development
./aws-infrastructure/deploy.sh dev

# Staging
./aws-infrastructure/deploy.sh staging

# Production
./aws-infrastructure/deploy.sh prod
```

### Check Stack Status
```bash
# Get stack status
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query 'Stacks[0].StackStatus'

# Get all outputs
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query 'Stacks[0].Outputs'

# Get specific output (API endpoint)
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" \
  --output text
```

### Delete Infrastructure
```bash
# Delete development stack
aws cloudformation delete-stack --stack-name voice-mode-stack-dev

# Wait for deletion
aws cloudformation wait stack-delete-complete --stack-name voice-mode-stack-dev

# List remaining stacks
aws cloudformation describe-stacks --query 'Stacks[].StackName'
```

## Lambda Commands

### Get Function Info
```bash
aws lambda get-function \
  --function-name voice-mode-transcribe-dev
```

### View Lambda Logs
```bash
# Real-time logs
aws logs tail /aws/lambda/voice-mode-transcribe-dev --follow

# Logs from last hour
aws logs filter-log-events \
  --log-group-name /aws/lambda/voice-mode-transcribe-dev \
  --start-time $(($(date +%s) - 3600))000

# Save logs
aws logs filter-log-events \
  --log-group-name /aws/lambda/voice-mode-transcribe-dev \
  > lambda-logs.json
```

### Update Lambda Code
```bash
# Build function
cd aws-lambda/transcribe
npm install --production
zip -r ../../function.zip .
cd ../..

# Update function
aws lambda update-function-code \
  --function-name voice-mode-transcribe-dev \
  --zip-file fileb://function.zip
```

## DynamoDB Commands

### Query Transcriptions
```bash
# Scan all items
aws dynamodb scan \
  --table-name voice-mode-transcriptions-dev

# Get specific item
aws dynamodb get-item \
  --table-name voice-mode-transcriptions-dev \
  --key '{"transcriptionId":{"S":"uuid-here"}}'

# Query by user ID
aws dynamodb query \
  --table-name voice-mode-transcriptions-dev \
  --index-name UserIdTimestampIndex \
  --key-condition-expression "userId = :uid" \
  --expression-attribute-values '{":uid":{"S":"user-id"}}'

# Count items
aws dynamodb scan \
  --table-name voice-mode-transcriptions-dev \
  --select COUNT
```

### Export Data
```bash
aws dynamodb scan \
  --table-name voice-mode-transcriptions-dev \
  --output json > transcriptions.json
```

## S3 Commands

### List Audio Files
```bash
# List all
aws s3 ls s3://voice-mode-audio-* --recursive

# List specific user
aws s3 ls s3://voice-mode-audio-*/audio/USER_ID/ --recursive

# Get summary
aws s3 ls s3://voice-mode-audio-* --recursive --summarize
```

### Download Audio Files
```bash
# Download single file
aws s3 cp s3://BUCKET/audio/user/file.m4a ./

# Download all for user
aws s3 sync s3://BUCKET/audio/USER_ID/ ./downloads/
```

### Delete Audio Files
```bash
# Delete single file
aws s3 rm s3://BUCKET/audio/user/file.m4a

# Delete all for user
aws s3 rm s3://BUCKET/audio/USER_ID/ --recursive
```

## API Testing Commands

### Get API Endpoint
```bash
export API_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" \
  --output text)

echo $API_ENDPOINT
```

### Test Health
```bash
curl -X POST "${API_ENDPOINT}/transcribe" \
  -H "Authorization: Bearer ${VOICE_MODE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "audio":"",
    "filename":"health.m4a",
    "userId":"test"
  }' -v
```

### Test with Real Audio
```bash
# Encode audio file
BASE64_AUDIO=$(base64 -i audio.m4a)

# Send request
curl -X POST "${API_ENDPOINT}/transcribe" \
  -H "Authorization: Bearer ${VOICE_MODE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "{
    \"audio\":\"${BASE64_AUDIO}\",
    \"filename\":\"test.m4a\",
    \"userId\":\"test-user\"
  }"
```

## CloudWatch Commands

### View Metrics
```bash
# Get Lambda duration
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Duration \
  --dimensions Name=FunctionName,Value=voice-mode-transcribe-dev \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum

# Get Lambda errors
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Errors \
  --dimensions Name=FunctionName,Value=voice-mode-transcribe-dev \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Sum
```

### View Alarms
```bash
aws cloudwatch describe-alarms \
  --alarm-names voice-mode-transcribe-errors-dev
```

## Billing & Cost Commands

### Check Costs
```bash
# Get cost for this month
aws ce get-cost-and-usage \
  --time-period Start=$(date +%Y-%m-01),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost

# Get cost by service
aws ce get-cost-and-usage \
  --time-period Start=$(date -d '30 days ago' +%Y-%m-%d),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --group-by Type=DIMENSION,Key=SERVICE
```

### Check Budget
```bash
aws budgets describe-budgets --account-id ACCOUNT_ID
```

## Git Commands

### View History
```bash
git log --oneline -20
git log --graph --oneline --all
git log --follow -- file.txt
```

### Create Feature Branch
```bash
git checkout -b feature/my-feature
# Make changes
git add .
git commit -m "Description"
git push origin feature/my-feature
```

### Merge to Main
```bash
git checkout main
git pull origin main
git merge feature/my-feature
git push origin main
```

### Rollback Commit
```bash
# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1

# Revert specific commit
git revert COMMIT_HASH
```

## GitHub Actions Commands

### Trigger Workflow
```bash
# Using GitHub CLI
gh workflow run deploy.yml \
  -f environment=dev

# Or via GitHub UI:
# 1. Go to Actions tab
# 2. Click "Deploy to AWS"
# 3. Click "Run workflow"
# 4. Select environment
# 5. Click "Run workflow"
```

### View Workflow Status
```bash
gh run list --repo lucasdickey/voice-mode
gh run view RUN_ID
gh run watch RUN_ID
```

## Useful One-Liners

### Deploy Everything
```bash
export OPENAI_API_KEY="sk-..." && \
export VOICE_MODE_API_KEY=$(openssl rand -hex 32) && \
./aws-infrastructure/deploy.sh dev && \
./gradlew clean assembleDebug && \
echo "Deployment complete!"
```

### Get All Resources Info
```bash
echo "=== API Endpoint ===" && \
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].Outputs" --output table && \
echo "\n=== Lambda Function ===" && \
aws lambda get-function --function-name voice-mode-transcribe-dev \
  --query 'Configuration.[FunctionName,Runtime,MemorySize,Timeout]' && \
echo "\n=== DynamoDB Table ===" && \
aws dynamodb describe-table \
  --table-name voice-mode-transcriptions-dev \
  --query 'Table.[TableName,TableStatus,TableSizeBytes,ItemCount]'
```

### Monitor Everything
```bash
watch -n 5 'echo "=== Lambda Errors ===" && \
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Errors \
  --dimensions Name=FunctionName,Value=voice-mode-transcribe-dev \
  --start-time $(date -u -d 5min ago +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 \
  --statistics Sum \
  --query "Datapoints[0].Sum"'
```

### Cleanup All
```bash
# Delete all stacks
for env in dev staging prod; do
  aws cloudformation delete-stack --stack-name voice-mode-stack-$env
done

# Wait for deletion
for env in dev staging prod; do
  aws cloudformation wait stack-delete-complete --stack-name voice-mode-stack-$env
done

echo "All stacks deleted"
```

## Troubleshooting Commands

### Check AWS Permissions
```bash
aws sts get-caller-identity
aws iam get-user
```

### Validate CloudFormation
```bash
aws cloudformation validate-template \
  --template-body file://aws-infrastructure/cloudformation.yaml
```

### Debug Lambda
```bash
# Test Lambda locally
aws lambda invoke \
  --function-name voice-mode-transcribe-dev \
  --payload '{"body":"{}"}' \
  response.json && cat response.json
```

### Check API Authorization
```bash
# Without auth (should fail)
curl -X POST "${API_ENDPOINT}/transcribe" \
  -H "Content-Type: application/json" \
  -d '{"audio":"","filename":"test.m4a"}'

# With correct auth (should work)
curl -X POST "${API_ENDPOINT}/transcribe" \
  -H "Authorization: Bearer ${VOICE_MODE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"audio":"","filename":"test.m4a"}'
```

## Performance Testing

### Load Test with Artillery
```bash
npm install -g artillery

# Create test file
cat > load-test.yml << 'EOF'
config:
  target: "{{ API_ENDPOINT }}"
  phases:
    - duration: 60
      arrivalRate: 10

scenarios:
  - name: "Transcribe"
    flow:
      - post:
          url: "/transcribe"
          headers:
            Authorization: "Bearer {{ API_KEY }}"
          json:
            audio: "SUQzBAAAAAAAI1..."
            filename: "test.m4a"
EOF

# Run test
API_ENDPOINT=$API_ENDPOINT API_KEY=$VOICE_MODE_API_KEY artillery run load-test.yml
```

---

**Print this page and keep it handy during development!**

Last Updated: November 13, 2024
