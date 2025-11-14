# Voice Mode - AWS Setup Guide

Complete guide for deploying Voice Mode using AWS services with OpenAI Whisper integration.

## Architecture Overview

```
┌──────────────────────────────┐
│   Android App                 │
│   (Voice Mode)                │
└──────────────┬────────────────┘
               │ HTTPS POST /transcribe
               │ (Base64 audio + API key)
               ▼
┌──────────────────────────────────────────────────┐
│        AWS API Gateway                           │
│        (Regional endpoint with auth)             │
└──────────────┬──────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────┐
│        AWS Lambda (nodejs18.x)                   │
│        ├─ Decode Base64 audio                    │
│        ├─ Upload to S3                           │
│        ├─ Call OpenAI Whisper API               │
│        └─ Store in DynamoDB                      │
└───┬──────────────────────┬──────────────────────┘
    │                      │
    ▼                      ▼
┌──────────────┐      ┌──────────────────┐
│ AWS S3       │      │ DynamoDB         │
│ Audio Files  │      │ Transcriptions   │
└──────────────┘      └──────────────────┘
    │
    └─→ OpenAI Whisper API
        (external service)
```

## Prerequisites

1. **AWS Account** with appropriate permissions
2. **OpenAI API Key** - for Whisper transcription
3. **AWS CLI** - configured locally
4. **Git** - for CI/CD
5. **Node.js 18+** - for Lambda development

## Step-by-Step Setup

### Phase 1: Prepare Credentials

#### 1.1 Get OpenAI API Key

1. Go to https://platform.openai.com/api-keys
2. Click "Create new secret key"
3. Copy the key (you'll only see it once)
4. Save it securely

#### 1.2 Generate Voice Mode API Key

Generate a secure random key for mobile app authentication:

```bash
# On macOS/Linux
openssl rand -hex 32

# This will output something like:
# a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
```

Copy this value - you'll use it as `VOICE_MODE_API_KEY`.

### Phase 2: Deploy Infrastructure

#### 2.1 Configure AWS CLI

```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Default region: us-east-1 (recommended)
# Default output format: json
```

#### 2.2 Set Environment Variables

```bash
export OPENAI_API_KEY="sk-..." # Your OpenAI API key
export VOICE_MODE_API_KEY="a1b2c3d4..." # Generated API key
export AWS_REGION="us-east-1"
```

#### 2.3 Deploy CloudFormation Stack

Automatic deployment script:

```bash
cd voice-mode
./aws-infrastructure/deploy.sh dev
```

Manual deployment:

```bash
aws cloudformation create-stack \
  --stack-name voice-mode-stack-dev \
  --template-body file://aws-infrastructure/cloudformation.yaml \
  --parameters \
    ParameterKey=Environment,ParameterValue=dev \
    ParameterKey=OpenAIApiKey,ParameterValue=$OPENAI_API_KEY \
    ParameterKey=ApiKeyValue,ParameterValue=$VOICE_MODE_API_KEY \
  --capabilities CAPABILITY_NAMED_IAM \
  --region $AWS_REGION
```

This creates:
- ✅ API Gateway endpoint
- ✅ Lambda function
- ✅ DynamoDB table
- ✅ S3 bucket
- ✅ CloudWatch logs
- ✅ IAM roles & policies

#### 2.4 Monitor Deployment

```bash
# Watch stack creation
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query 'Stacks[0].[StackStatus,StackStatusReason]'

# Get outputs (API endpoint, etc.)
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query 'Stacks[0].Outputs'
```

### Phase 3: Update Android App

#### 3.1 Get API Endpoint

After successful deployment, retrieve your API endpoint:

```bash
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" \
  --output text
```

Output will be like: `https://xxxxx.execute-api.us-east-1.amazonaws.com/dev`

#### 3.2 Update BedrockService

Edit `app/src/main/java/com/voicemode/aws/BedrockService.kt`:

```kotlin
private val apiEndpoint = "https://xxxxx.execute-api.us-east-1.amazonaws.com/dev"
private val apiKey = "a1b2c3d4..."
```

Or better yet, use ConfigManager for secure storage:

```kotlin
val configManager = ConfigManager(context)
configManager.setApiEndpoint("https://xxxxx.execute-api.us-east-1.amazonaws.com/dev")
configManager.setApiKey("a1b2c3d4...")
```

#### 3.3 Rebuild APK

```bash
./gradlew clean assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Phase 4: Test the Setup

#### 4.1 Test API Directly

```bash
# Create a test audio file (base64 encoded silence)
API_ENDPOINT="https://xxxxx.execute-api.us-east-1.amazonaws.com/dev"
API_KEY="a1b2c3d4..."

# Create minimal MP4 audio and encode
# (In practice, you'd use real audio)

curl -X POST "${API_ENDPOINT}/transcribe" \
  -H "Authorization: Bearer ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "audio":"SUQzBAAAAAAAI1NTU...",
    "filename":"test.m4a",
    "userId":"test-user"
  }'
```

#### 4.2 Test from Android App

1. Enable microphone permission
2. Open any text input field
3. Tap the 🎤 FAB button
4. Say something ("hello world", etc.)
5. Wait 3-5 seconds
6. Check logs for transcription:

```bash
adb logcat | grep -i "transcribe\|bedrock\|cloud"
```

#### 4.3 Monitor in CloudWatch

```bash
# View Lambda logs in real-time
aws logs tail /aws/lambda/voice-mode-transcribe-dev --follow
```

## AWS Services Overview

### AWS Lambda

**What it does:** Processes audio transcription requests

**Configuration:**
- Runtime: Node.js 18.x
- Memory: 1024 MB
- Timeout: 300 seconds
- Triggered by: API Gateway

**Environment Variables:**
- `OPENAI_API_KEY`: OpenAI API key
- `DYNAMODB_TABLE`: Table name
- `S3_BUCKET`: Audio storage bucket

**Cost:** Pay per invocation ($0.20 per 1M requests + duration)

### API Gateway

**What it does:** Provides HTTPS endpoint for mobile app

**Features:**
- Custom authorizer with API key validation
- CORS enabled
- Regional endpoint (lower latency)
- CloudWatch integration

**Cost:** Pay per API call ($3.50 per 1M requests)

### DynamoDB

**What it does:** Stores transcription history

**Schema:**
```
Primary Key: transcriptionId
Global Secondary Index: userId-timestamp (for user history)
Attributes:
  - transcriptionId (String, PK)
  - userId (String, GSI PK)
  - timestamp (String, GSI SK)
  - audioS3Key (String)
  - transcription (String)
  - confidence (Number)
  - language (String)
  - ttl (Number) - auto-delete after 90 days
```

**Cost:** On-demand pricing ($1.25 per 1M write units + $0.25 per 1M read units)

### S3

**What it does:** Stores audio files for 90 days

**Configuration:**
- Versioning enabled
- Server-side encryption (AES-256)
- Lifecycle: Auto-delete after 90 days
- Privacy: Blocked public access

**Cost:** ~$0.023 per GB stored

### CloudWatch

**What it does:** Logs and monitoring

**Includes:**
- Lambda execution logs
- API Gateway logs
- Performance metrics
- Error alarms

**Cost:** $0.50 per 1GB ingested + $0.03 per 1GB stored

## Environment Management

### Development

```bash
./aws-infrastructure/deploy.sh dev
```

### Staging

```bash
./aws-infrastructure/deploy.sh staging
```

### Production

```bash
./aws-infrastructure/deploy.sh prod
```

Each environment has:
- Separate CloudFormation stack
- Isolated DynamoDB table
- Isolated S3 bucket
- Separate Lambda function
- Independent CloudWatch logs

## Cost Estimation

### Monthly Costs (Rough Estimates)

**Assumptions:** 10,000 transcriptions/month, avg 30 seconds audio

| Service | Cost |
|---------|------|
| Lambda | ~$2.00 |
| API Gateway | ~$0.04 |
| DynamoDB | ~$1.00 |
| S3 | ~$0.50 |
| CloudWatch | ~$1.00 |
| OpenAI Whisper | ~$100 (0.001 min/request) |
| **Total** | **~$104.50/month** |

**Free tier usage:** AWS free tier covers most services for first 12 months

## Scaling Considerations

### Bottlenecks

1. **API Gateway rate limiting** - 10,000 requests/sec (can request increase)
2. **Lambda concurrent executions** - 1000 (can request increase)
3. **OpenAI Whisper rate limits** - Check OpenAI documentation

### Auto-scaling

- **Lambda:** Automatic (concurrent executions scale on demand)
- **DynamoDB:** On-demand billing (auto-scales)
- **API Gateway:** Managed by AWS

## Troubleshooting

### Lambda errors in CloudWatch

```bash
aws logs tail /aws/lambda/voice-mode-transcribe-dev --follow
```

### API Gateway issues

```bash
# Test with curl
curl -v -X POST https://xxxxx.execute-api.us-east-1.amazonaws.com/dev/transcribe \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"audio":"...", "filename":"test.m4a"}'
```

### Check DynamoDB data

```bash
aws dynamodb scan \
  --table-name voice-mode-transcriptions-dev \
  --region us-east-1
```

### Monitor costs

```bash
# AWS Billing Dashboard:
# https://console.aws.amazon.com/billing/

# CLI:
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity DAILY \
  --metrics UnblendedCost \
  --group-by Type=DIMENSION,Key=SERVICE
```

## CI/CD with GitHub Actions

See `.github/workflows/deploy.yml` for automated deployment.

### Setup GitHub Actions

1. Create IAM role for GitHub Actions
2. Add secrets to repository:
   - `AWS_ROLE_ARN`
   - `OPENAI_API_KEY`
   - `VOICE_MODE_API_KEY`

3. Push to main branch to trigger deployment

## Next Steps

1. ✅ Deploy infrastructure
2. ✅ Test Lambda function
3. ✅ Update Android app
4. ✅ Test end-to-end
5. 🚀 Monitor CloudWatch metrics
6. 🚀 Implement CI/CD with GitHub Actions
7. 🚀 Add API rate limiting
8. 🚀 Implement user authentication (Cognito)
9. 🚀 Add transcription export features
10. 🚀 Set up production environment

## References

- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [AWS API Gateway Documentation](https://docs.aws.amazon.com/apigateway/)
- [AWS DynamoDB Documentation](https://docs.aws.amazon.com/dynamodb/)
- [OpenAI Whisper API](https://platform.openai.com/docs/guides/speech-to-text)
- [AWS CLI Reference](https://docs.aws.amazon.com/cli/)

## Support

For issues:
1. Check CloudWatch logs
2. Review error messages in Lambda console
3. Test API endpoint with curl
4. Verify environment variables
5. Check AWS IAM permissions
