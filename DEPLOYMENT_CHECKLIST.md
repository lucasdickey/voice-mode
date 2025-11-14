# Voice Mode - Deployment Checklist

Complete checklist for deploying Voice Mode to production.

## Pre-Deployment (30 minutes)

### AWS Account Setup
- [ ] AWS Account created and verified
- [ ] AWS CLI installed and configured
- [ ] AWS credentials configured locally (`aws configure`)
- [ ] Default region set to us-east-1

### API Keys & Credentials
- [ ] OpenAI API key obtained from https://platform.openai.com/api-keys
- [ ] OpenAI account has sufficient credits
- [ ] Voice Mode API key generated (`openssl rand -hex 32`)
- [ ] Credentials stored securely (not in code/git)

### Local Environment
- [ ] Node.js 18+ installed (`node --version`)
- [ ] npm installed (`npm --version`)
- [ ] Android SDK installed
- [ ] Android Studio installed
- [ ] Git repository cloned and updated

## Development Environment Deployment (15 minutes)

### Deploy Infrastructure

```bash
# Set environment variables
export OPENAI_API_KEY="sk-..."
export VOICE_MODE_API_KEY="a1b2c3d4..."
export AWS_REGION="us-east-1"

# Run deployment script
./aws-infrastructure/deploy.sh dev

# Wait for completion (2-5 minutes)
# CloudFormation shows stack creation progress
```

### Verify Deployment

- [ ] CloudFormation stack created successfully
  ```bash
  aws cloudformation describe-stacks \
    --stack-name voice-mode-stack-dev \
    --query 'Stacks[0].StackStatus'
  ```

- [ ] Lambda function deployed
  ```bash
  aws lambda get-function \
    --function-name voice-mode-transcribe-dev
  ```

- [ ] DynamoDB table created
  ```bash
  aws dynamodb describe-table \
    --table-name voice-mode-transcriptions-dev
  ```

- [ ] S3 bucket created
  ```bash
  aws s3 ls | grep voice-mode-audio
  ```

- [ ] API Gateway endpoint available
  ```bash
  aws cloudformation describe-stacks \
    --stack-name voice-mode-stack-dev \
    --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue"
  ```

### Test Lambda Function

```bash
# Get API endpoint
API_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" \
  --output text)

# Test with curl
curl -X POST "${API_ENDPOINT}/transcribe" \
  -H "Authorization: Bearer ${VOICE_MODE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "audio":"SUQzBAAAAAAAI1NTU...",
    "filename":"test.m4a",
    "userId":"dev-user"
  }'
```

- [ ] API responds with 200 OK
- [ ] Response includes transcriptionId
- [ ] No authentication errors
- [ ] CloudWatch logs show successful invocation

### Update Android App

- [ ] Update BedrockService with API endpoint
  ```kotlin
  private val apiEndpoint = "YOUR_API_ENDPOINT"
  private val apiKey = "YOUR_API_KEY"
  ```

- [ ] Or use ConfigManager for secure storage
  ```kotlin
  val configManager = ConfigManager(context)
  configManager.setApiEndpoint(apiEndpoint)
  configManager.setApiKey(apiKey)
  ```

### Build & Test APK

- [ ] Build debug APK
  ```bash
  ./gradlew clean assembleDebug
  ```

- [ ] Install on device
  ```bash
  adb install app/build/outputs/apk/debug/app-debug.apk
  ```

- [ ] Test basic functionality
  - [ ] App installs without errors
  - [ ] Accessibility service enables
  - [ ] FAB appears in text fields
  - [ ] Microphone permission granted
  - [ ] Recording starts/stops correctly

### Test End-to-End

- [ ] Test transcription
  1. Open text input field
  2. Tap 🎤 FAB button
  3. Speak clearly ("Hello world")
  4. Wait for completion
  5. Check logs for transcription result

- [ ] Verify in CloudWatch
  ```bash
  aws logs tail /aws/lambda/voice-mode-transcribe-dev --follow
  ```

- [ ] Verify in DynamoDB
  ```bash
  aws dynamodb scan --table-name voice-mode-transcriptions-dev
  ```

## Staging Environment Deployment (10 minutes)

### Deploy to Staging

- [ ] Code merged to main branch
- [ ] All tests passing

```bash
./aws-infrastructure/deploy.sh staging

# Or trigger via GitHub Actions:
# 1. Go to Actions tab
# 2. Click "Deploy to AWS"
# 3. Click "Run workflow"
# 4. Select "staging"
# 5. Click "Run workflow"
```

- [ ] CloudFormation stack `voice-mode-stack-staging` created
- [ ] Lambda function `voice-mode-transcribe-staging` deployed
- [ ] DynamoDB table `voice-mode-transcriptions-staging` created
- [ ] S3 bucket `voice-mode-audio-*-staging` created

### Test Staging Environment

- [ ] Update app with staging endpoint
- [ ] Rebuild APK with staging config
- [ ] Test on device with staging API
- [ ] Verify transcriptions stored in staging DB
- [ ] Check staging CloudWatch logs

## Production Environment Deployment (15 minutes)

### Pre-Production Checklist

- [ ] All development tests passing
- [ ] All staging tests passing
- [ ] Code reviewed and approved
- [ ] API key rotated if necessary
- [ ] Cost estimates reviewed
- [ ] Backup strategy documented
- [ ] Monitoring and alarms configured
- [ ] Support plan documented

### Deploy to Production

**WARNING: Production deployment is permanent and will incur costs**

```bash
./aws-infrastructure/deploy.sh prod

# Or trigger via GitHub Actions:
# 1. Go to Actions tab
# 2. Click "Deploy to AWS"
# 3. Click "Run workflow"
# 4. Select "prod"
# 5. Click "Run workflow" (requires approval)
```

- [ ] CloudFormation stack `voice-mode-stack-prod` created
- [ ] Lambda function `voice-mode-transcribe-prod` deployed
- [ ] DynamoDB table `voice-mode-transcriptions-prod` created
- [ ] S3 bucket `voice-mode-audio-*-prod` created
- [ ] API Gateway endpoint in production

### Production Verification

- [ ] API endpoint accessible
- [ ] Authentication working
- [ ] Database connected
- [ ] S3 storage working
- [ ] CloudWatch monitoring active
- [ ] Alarms configured and testing
- [ ] Logs flowing to CloudWatch
- [ ] Cost monitoring enabled

### Post-Deployment

- [ ] Update app release notes with new endpoint
- [ ] Build production APK with prod endpoint
- [ ] Test production APK thoroughly
- [ ] Prepare release notes
- [ ] Notify stakeholders

## Monitoring & Maintenance

### Daily Checks

```bash
# Check API health
curl -X POST "YOUR_PROD_ENDPOINT/transcribe" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"audio":"","filename":"health.m4a"}'

# View error logs
aws logs tail /aws/lambda/voice-mode-transcribe-prod --follow
```

- [ ] API responding normally
- [ ] No Lambda errors
- [ ] DynamoDB accepting writes
- [ ] S3 storage working
- [ ] CloudWatch alarms not triggered

### Weekly Checks

```bash
# Check costs
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-07 \
  --granularity DAILY \
  --metrics UnblendedCost

# Check database size
aws dynamodb describe-table \
  --table-name voice-mode-transcriptions-prod \
  --query 'Table.TableSizeBytes'

# Check S3 storage
aws s3 ls s3://voice-mode-audio-* --recursive --summarize
```

- [ ] Costs within budget
- [ ] Database size manageable
- [ ] S3 storage within limits
- [ ] No quota approaching
- [ ] Performance metrics normal

### Monthly Checks

- [ ] Review CloudWatch metrics
- [ ] Check Lambda duration trends
- [ ] Analyze API usage patterns
- [ ] Review error logs for issues
- [ ] Update documentation if needed
- [ ] Plan capacity if needed
- [ ] Review security configuration

## Rollback Procedure

If production deployment has issues:

```bash
# Delete problematic stack
aws cloudformation delete-stack --stack-name voice-mode-stack-prod

# Wait for deletion
aws cloudformation wait stack-delete-complete --stack-name voice-mode-stack-prod

# Redeploy with previous version
git checkout PREVIOUS_COMMIT
./aws-infrastructure/deploy.sh prod
```

- [ ] Identify issue
- [ ] Back out change
- [ ] Document incident
- [ ] Implement fix
- [ ] Test in dev/staging
- [ ] Redeploy to production

## Scaling Checklist

If you need more capacity:

### Lambda Scaling

- [ ] Monitor Lambda concurrent executions
  ```bash
  aws cloudwatch get-metric-statistics \
    --namespace AWS/Lambda \
    --metric-name ConcurrentExecutions
  ```

- [ ] Request limit increase if needed
  ```bash
  aws service-quotas request-service-quota-increase \
    --service-code lambda \
    --quota-code L-4FBB7105 \
    --desired-value 5000
  ```

### DynamoDB Scaling

- [ ] Monitor read/write capacity
  ```bash
  aws cloudwatch get-metric-statistics \
    --namespace AWS/DynamoDB \
    --metric-name ConsumedWriteCapacityUnits
  ```

- [ ] Switch to on-demand if cost-effective
  ```bash
  aws dynamodb update-billing-mode \
    --table-name voice-mode-transcriptions-prod \
    --billing-mode PAY_PER_REQUEST
  ```

### S3 Scaling

- [ ] Monitor storage usage
  ```bash
  aws s3 ls s3://voice-mode-audio-* --recursive --summarize
  ```

- [ ] Implement retention policies if needed
- [ ] Archive old data if desired

## Cost Management

### Budget Alert Setup

```bash
# Set monthly budget alert
aws budgets create-budget \
  --account-id ACCOUNT_ID \
  --budget file://budget.json
```

- [ ] Budget limit: $500/month
- [ ] Alert at 50%, 80%, 100%
- [ ] Notification email configured

### Cost Optimization

- [ ] Review data transfer costs
- [ ] Check for unused resources
- [ ] Consider Reserved Capacity if usage is stable
- [ ] Review Lambda duration vs. memory allocation
- [ ] Optimize API Gateway caching

## Troubleshooting Guide

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Lambda timeout | Large audio file | Increase timeout, optimize Whisper call |
| DynamoDB throttling | High write volume | Switch to on-demand billing |
| API 403 Unauthorized | Wrong API key | Verify VOICE_MODE_API_KEY matches |
| S3 access denied | IAM permission issue | Check Lambda execution role |
| CloudFormation failure | Template error | Check CloudFormation events tab |

### Getting Help

1. Check CloudWatch logs
2. Review CloudFormation events
3. Check AWS IAM permissions
4. Verify all credentials
5. Test with curl directly
6. Check GitHub Actions logs
7. Review AWS service health

## Success Criteria

- [x] Android app compiles successfully
- [x] AWS infrastructure deploys without errors
- [x] API endpoint accessible and authenticated
- [x] Lambda function processes audio
- [x] OpenAI Whisper returns transcriptions
- [x] DynamoDB stores results
- [x] S3 stores audio files
- [x] CloudWatch logs all events
- [x] All services monitored and alarmed
- [x] Documentation complete
- [x] Ready for production use

## Post-Launch

### Week 1
- [ ] Monitor for issues
- [ ] Gather user feedback
- [ ] Track error rates
- [ ] Review performance metrics
- [ ] Plan improvements

### Month 1
- [ ] Analyze usage patterns
- [ ] Optimize costs if needed
- [ ] Plan feature updates
- [ ] Document lessons learned

---

**Deployment Status**: ✅ Ready for production

**Last Updated**: November 13, 2024

**Next Steps**: Deploy to AWS and launch!
