# Voice Mode - Complete SaaS/IaaS Configuration Guide

Comprehensive, step-by-step guide to configure all external services and AWS infrastructure.

**Estimated Time**: 2-3 hours total

---

## Phase 1: OpenAI API Setup (15 minutes)

### Step 1.1: Create OpenAI Account

1. Go to https://platform.openai.com/signup
2. Sign up with email or Google account
3. Complete email verification
4. Fill in organization info (optional)

### Step 1.2: Add Payment Method

1. Go to https://platform.openai.com/account/billing/overview
2. Click "Set up paid account"
3. Enter billing email
4. Add payment method (credit/debit card)
5. Click "Set up paid account"

**Important**: OpenAI charges per minute of audio:
- Whisper API: ~$0.02 per minute
- 1 hour audio = ~$1.20

### Step 1.3: Create API Key

1. Go to https://platform.openai.com/api-keys
2. Click "Create new secret key"
3. Name it: "voice-mode-app"
4. Click "Create secret key"
5. **COPY THE KEY** (you only see it once!)
   - Format: `sk-...` (about 48 characters)

**Save this securely:**
```
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Step 1.4: Check API Quota

1. Go to https://platform.openai.com/account/billing/limits
2. Verify:
   - Usage limits are set (optional)
   - Billing alerts are enabled
   - Hard limit is high enough

### Step 1.5: Test OpenAI API

```bash
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer sk-YOUR_KEY"

# Should return list of available models
```

**Expected Response:**
```json
{
  "object": "list",
  "data": [
    {"id": "whisper-1", "object": "model", ...},
    ...
  ]
}
```

✅ **OpenAI Setup Complete**

---

## Phase 2: AWS Account Setup (30 minutes)

### Step 2.1: Create AWS Account

1. Go to https://aws.amazon.com/
2. Click "Create an AWS Account"
3. Enter email address
4. Create password (strong password required)
5. Enter account name
6. Enter contact information
7. Enter payment method (credit/debit card)
8. Verify phone number (receive SMS/call)
9. Verify email address

### Step 2.2: Enable AWS Billing Alerts

1. Go to AWS Console
2. Click your account name (top right)
3. Click "Billing and cost management"
4. Click "Billing preferences"
5. Enable:
   - ☑️ Receive PDF Invoice by Email
   - ☑️ Receive Billing Alerts
6. Save preferences

### Step 2.3: Create AWS IAM User

**Why**: Don't use root credentials for daily work

1. Go to AWS Console > IAM
2. Click "Users" in left menu
3. Click "Create user"
4. Username: `voice-mode-admin`
5. Click "Next"
6. Click "Attach policies directly"
7. Search for and select:
   - ☑️ `AdministratorAccess`
8. Click "Next"
9. Click "Create user"

### Step 2.4: Create Access Keys for CLI

1. Go to AWS Console > IAM > Users
2. Click `voice-mode-admin`
3. Click "Security credentials" tab
4. Scroll to "Access keys"
5. Click "Create access key"
6. Select "Command Line Interface (CLI)"
7. Click "Next"
8. Click "Create access key"
9. **COPY AND SAVE:**
   ```
   AWS_ACCESS_KEY_ID=AKIA...
   AWS_SECRET_ACCESS_KEY=...
   ```
10. Click "Done"

**⚠️ WARNING**: Never share these keys! Store securely.

### Step 2.5: Configure AWS CLI

1. Install AWS CLI:
   ```bash
   # macOS with Homebrew
   brew install awscli

   # Or download from: https://aws.amazon.com/cli/
   ```

2. Verify installation:
   ```bash
   aws --version
   ```

3. Configure credentials:
   ```bash
   aws configure
   ```

4. Enter when prompted:
   ```
   AWS Access Key ID: AKIA...
   AWS Secret Access Key: ...
   Default region name: us-east-1
   Default output format: json
   ```

5. Verify configuration:
   ```bash
   aws sts get-caller-identity

   # Should output:
   # {
   #     "UserId": "...",
   #     "Account": "123456789012",
   #     "Arn": "arn:aws:iam::123456789012:user/voice-mode-admin"
   # }
   ```

**Save your Account ID** (12-digit number in Arn):
```
AWS_ACCOUNT_ID=123456789012
```

### Step 2.6: Set Up CloudWatch Alarms

1. Go to AWS Console > CloudWatch
2. Click "Alarms" > "Create Alarm"
3. Select metric: "Estimated Charges"
4. Set threshold: $50
5. Action: Email notification
6. Enter your email
7. Create alarm
8. Verify email in inbox

✅ **AWS Account Setup Complete**

---

## Phase 3: Generate Voice Mode API Key (5 minutes)

### Step 3.1: Generate Secure API Key

Open terminal and run:

```bash
# macOS/Linux
openssl rand -hex 32

# Windows PowerShell
[System.Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLower()
```

**Example Output:**
```
a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
```

**Save this securely:**
```
VOICE_MODE_API_KEY=a1b2c3d4e5f6...
```

### Step 3.2: Store Credentials Securely

Create a `.env.local` file (never commit to git):

```bash
# .env.local (ADD TO .gitignore)
export OPENAI_API_KEY="sk-..."
export VOICE_MODE_API_KEY="a1b2c3d4..."
export AWS_ACCOUNT_ID="123456789012"
export AWS_REGION="us-east-1"
```

Load credentials:
```bash
source .env.local
echo $OPENAI_API_KEY  # Should show your key
```

✅ **API Keys Ready**

---

## Phase 4: Deploy AWS Infrastructure (45 minutes)

### Step 4.1: Validate CloudFormation Template

```bash
cd voice-mode

aws cloudformation validate-template \
  --template-body file://aws-infrastructure/cloudformation.yaml \
  --region $AWS_REGION

# Should output: "TemplateDescription": "Voice Mode..."
```

### Step 4.2: Deploy CloudFormation Stack

```bash
# Create the stack
aws cloudformation create-stack \
  --stack-name voice-mode-stack-dev \
  --template-body file://aws-infrastructure/cloudformation.yaml \
  --parameters \
    ParameterKey=Environment,ParameterValue=dev \
    ParameterKey=OpenAIApiKey,ParameterValue=$OPENAI_API_KEY \
    ParameterKey=ApiKeyValue,ParameterValue=$VOICE_MODE_API_KEY \
  --capabilities CAPABILITY_NAMED_IAM \
  --region $AWS_REGION

# Output:
# {
#     "StackId": "arn:aws:cloudformation:us-east-1:123456789012:stack/voice-mode-stack-dev/..."
# }
```

### Step 4.3: Monitor Stack Creation

```bash
# Watch in real-time (if you have 'watch' command)
watch -n 5 'aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].[StackStatus,StackStatusReason]" \
  --region $AWS_REGION'

# Or check manually (takes 3-5 minutes)
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --region $AWS_REGION
```

**Expected Statuses:**
- `CREATE_IN_PROGRESS` → (wait)
- `CREATE_COMPLETE` → ✅ Success!

### Step 4.4: Get Stack Outputs

```bash
# Get all outputs
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].Outputs" \
  --region $AWS_REGION \
  --output table

# Get specific output (API endpoint)
API_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" \
  --output text \
  --region $AWS_REGION)

echo $API_ENDPOINT
# Output: https://xxxxx.execute-api.us-east-1.amazonaws.com/dev
```

**Save the API Endpoint:**
```
VOICE_MODE_API_ENDPOINT=https://xxxxx.execute-api.us-east-1.amazonaws.com/dev
```

### Step 4.5: Verify AWS Resources

```bash
# Check Lambda function
aws lambda get-function \
  --function-name voice-mode-transcribe-dev \
  --region $AWS_REGION

# Check DynamoDB table
aws dynamodb describe-table \
  --table-name voice-mode-transcriptions-dev \
  --region $AWS_REGION

# Check S3 bucket
aws s3 ls | grep voice-mode-audio

# Check API Gateway
aws apigateway get-rest-apis \
  --region $AWS_REGION
```

### Step 4.6: Test API Gateway

```bash
# Test without authentication (should fail with 403)
curl -X POST "$API_ENDPOINT/transcribe" \
  -H "Content-Type: application/json" \
  -d '{"audio":"","filename":"test.m4a"}'

# Expected response: 403 Forbidden or "Unauthorized"

# Test with correct API key (will fail on audio but that's ok)
curl -X POST "$API_ENDPOINT/transcribe" \
  -H "Authorization: Bearer $VOICE_MODE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "audio":"SUQzBAAAAAAAI1NTU...",
    "filename":"test.m4a",
    "userId":"test"
  }' -v

# Should see 500 or error about invalid audio (that's ok for now)
# Important: No 403 error means auth is working!
```

✅ **AWS Infrastructure Deployed**

---

## Phase 5: Configure GitHub for CI/CD (45 minutes)

### Step 5.1: Set Up AWS OIDC Provider

**Why**: GitHub can authenticate with AWS without storing credentials

```bash
# Create OIDC provider
aws iam create-open-id-connect-provider \
  --url "https://token.actions.githubusercontent.com" \
  --client-id-list "sts.amazonaws.com" \
  --thumbprint-list "6938fd4d98bab03faadb97b34396831e3780aca1" \
  --region us-east-1

# Output:
# "OpenIDConnectProviderArn": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
```

**Save the ARN:**
```
AWS_OIDC_PROVIDER_ARN=arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com
```

### Step 5.2: Create GitHub OIDC Role

Create trust policy file:

```bash
cat > /tmp/trust-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:GITHUB_USER/voice-mode:ref:refs/heads/main"
        }
      }
    }
  ]
}
EOF

# Replace ACCOUNT_ID and GITHUB_USER:
sed -i "s/ACCOUNT_ID/$AWS_ACCOUNT_ID/g" /tmp/trust-policy.json
sed -i "s/GITHUB_USER/YOUR_GITHUB_USERNAME/g" /tmp/trust-policy.json
```

Create the role:

```bash
aws iam create-role \
  --role-name VoiceModeGitHubActionsRole \
  --assume-role-policy-document file:///tmp/trust-policy.json \
  --region us-east-1

# Output:
# "Arn": "arn:aws:iam::123456789012:role/VoiceModeGitHubActionsRole"
```

**Save the Role ARN:**
```
AWS_GITHUB_ACTIONS_ROLE=arn:aws:iam::123456789012:role/VoiceModeGitHubActionsRole
```

### Step 5.3: Add Permissions to GitHub Role

Create policy:

```bash
cat > /tmp/github-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudformation:*",
        "iam:*",
        "lambda:*",
        "apigateway:*",
        "dynamodb:*",
        "s3:*",
        "logs:*"
      ],
      "Resource": "*"
    }
  ]
}
EOF

# Attach policy to role
aws iam put-role-policy \
  --role-name VoiceModeGitHubActionsRole \
  --policy-name VoiceModeDeployment \
  --policy-document file:///tmp/github-policy.json \
  --region us-east-1
```

### Step 5.4: Add GitHub Secrets

1. Go to your GitHub repository: https://github.com/lucasdickey/voice-mode
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

Add these secrets:

| Secret Name | Value |
|-------------|-------|
| `AWS_ROLE_ARN` | `arn:aws:iam::123456789012:role/VoiceModeGitHubActionsRole` |
| `OPENAI_API_KEY` | `sk-xxxx...` (your OpenAI key) |
| `VOICE_MODE_API_KEY` | `a1b2c3d4...` (generated API key) |

**To add each secret:**
1. Click "New repository secret"
2. Name: (copy from table above)
3. Secret: (copy value from above)
4. Click "Add secret"

**Verify all 3 are added:**
- ✅ AWS_ROLE_ARN
- ✅ OPENAI_API_KEY
- ✅ VOICE_MODE_API_KEY

### Step 5.5: Test GitHub Actions Manually

1. Go to GitHub repository
2. Click **Actions** tab
3. Click **Deploy to AWS** workflow
4. Click **Run workflow** button
5. Environment: `dev`
6. Click **Run workflow**

**Monitor the deployment:**
1. Watch the workflow run
2. Check the logs in real-time
3. Should complete in ~5-10 minutes

**Check results:**
```bash
# Verify deployment
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].StackStatus"

# Should show: CREATE_COMPLETE
```

✅ **GitHub CI/CD Configured**

---

## Phase 6: Configure Android App (20 minutes)

### Step 6.1: Update API Endpoint

Edit `app/src/main/java/com/voicemode/aws/BedrockService.kt`:

```kotlin
class BedrockService(
    private val apiEndpoint: String,
    private val apiKey: String
) {
    // Implementation...
}
```

Edit `app/src/main/java/com/voicemode/VoiceModeAccessibilityService.kt`:

Find this function:
```kotlin
private fun getBedrockApiEndpoint(): String {
    return "http://your-backend-api.com"  // ← CHANGE THIS
}

private fun getBedrockApiKey(): String {
    return "your-api-key"  // ← CHANGE THIS
}
```

Replace with:
```kotlin
private fun getBedrockApiEndpoint(): String {
    return "https://xxxxx.execute-api.us-east-1.amazonaws.com/dev"  // Your API endpoint
}

private fun getBedrockApiKey(): String {
    return "a1b2c3d4..."  // Your VOICE_MODE_API_KEY
}
```

### Step 6.2: Better: Use ConfigManager (Recommended)

Instead of hardcoding, use secure storage:

```kotlin
// In MainActivity.kt onCreate():
val configManager = ConfigManager(context)
configManager.setApiEndpoint("https://xxxxx.execute-api.us-east-1.amazonaws.com/dev")
configManager.setApiKey("a1b2c3d4...")

// Then in BedrockService:
val endpoint = configManager.getApiEndpoint()
val apiKey = configManager.getApiKey()
```

### Step 6.3: Build Debug APK

```bash
./gradlew clean assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk (8.9 MB)
```

### Step 6.4: Install on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk

# Or reinstall:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 6.5: Grant Permissions

1. Open app
2. Tap "Enable Accessibility Service" button
3. Go to Settings → Accessibility
4. Find "Voice Mode"
5. Enable it
6. When prompted, grant microphone permission

✅ **Android App Configured**

---

## Phase 7: End-to-End Testing (30 minutes)

### Step 7.1: Test Android App

1. Open any app with text input (Notes, Email, etc.)
2. Focus on a text field
3. You should see a 🎤 FAB button appear
4. Tap the button
5. Speak: "Hello world"
6. Wait 3-5 seconds
7. Check logs:

```bash
adb logcat | grep -i "transcribe\|cloud\|bedrock"
```

### Step 7.2: Monitor Lambda

```bash
# View Lambda logs
aws logs tail /aws/lambda/voice-mode-transcribe-dev --follow

# Should show:
# - Receiving request
# - Uploading to S3
# - Calling OpenAI
# - Storing in DynamoDB
# - Returning response
```

### Step 7.3: Check DynamoDB

```bash
# Scan transcriptions table
aws dynamodb scan \
  --table-name voice-mode-transcriptions-dev \
  --region $AWS_REGION

# Should show entries with your transcriptions
```

### Step 7.4: Check S3

```bash
# List audio files
aws s3 ls s3://voice-mode-audio-* --recursive

# Should show your recorded audio files
```

### Step 7.5: Check CloudWatch Metrics

1. Go to AWS Console
2. CloudWatch → Dashboards
3. Look for Lambda metrics:
   - Duration (should be 10-30 seconds)
   - Errors (should be 0)
   - Invocations (should match your tests)

### Step 7.6: Test Different Scenarios

**Test 1: Clean speech**
- Speak: "Hello world"
- Expect: "hello world" (lowercased)

**Test 2: With pauses**
- Speak: "I am... testing... Voice Mode"
- Expect: "I am testing Voice Mode"

**Test 3: With numbers**
- Speak: "My phone number is five five five one two three four"
- Expect: Transcribed with numbers or words

**Test 4: With punctuation**
- Speak: "Question mark? Exclamation point!"
- Expect: Some punctuation detection

✅ **All Systems Testing Complete**

---

## Phase 8: Production Setup (30 minutes - Optional)

If you want to deploy to staging/production:

### Step 8.1: Create Staging Stack

```bash
aws cloudformation create-stack \
  --stack-name voice-mode-stack-staging \
  --template-body file://aws-infrastructure/cloudformation.yaml \
  --parameters \
    ParameterKey=Environment,ParameterValue=staging \
    ParameterKey=OpenAIApiKey,ParameterValue=$OPENAI_API_KEY \
    ParameterKey=ApiKeyValue,ParameterValue=$VOICE_MODE_API_KEY \
  --capabilities CAPABILITY_NAMED_IAM \
  --region $AWS_REGION

# Get staging endpoint
STAGING_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-staging \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" \
  --output text)

echo $STAGING_ENDPOINT
```

### Step 8.2: Create Production Stack

```bash
# NOTE: Be very careful with production!
aws cloudformation create-stack \
  --stack-name voice-mode-stack-prod \
  --template-body file://aws-infrastructure/cloudformation.yaml \
  --parameters \
    ParameterKey=Environment,ParameterValue=prod \
    ParameterKey=OpenAIApiKey,ParameterValue=$OPENAI_API_KEY \
    ParameterKey=ApiKeyValue,ParameterValue=$VOICE_MODE_API_KEY \
  --capabilities CAPABILITY_NAMED_IAM \
  --region $AWS_REGION

# Get production endpoint
PROD_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-prod \
  --query "Stacks[0].Outputs[?OutputKey=='ApiEndpoint'].OutputValue" \
  --output text)

echo $PROD_ENDPOINT
```

### Step 8.3: Monitor Costs

```bash
# Check current month costs
aws ce get-cost-and-usage \
  --time-period Start=$(date +%Y-%m-01),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics UnblendedCost \
  --group-by Type=DIMENSION,Key=SERVICE
```

✅ **Production Ready (Optional)**

---

## Final Verification Checklist

### Credentials ✅
- [ ] OpenAI API key working
- [ ] AWS credentials configured
- [ ] Voice Mode API key generated
- [ ] All stored securely (not in git)

### AWS Infrastructure ✅
- [ ] CloudFormation stack created
- [ ] Lambda function deployed
- [ ] API Gateway endpoint accessible
- [ ] DynamoDB table created
- [ ] S3 bucket created
- [ ] CloudWatch logs flowing

### GitHub CI/CD ✅
- [ ] OIDC provider created
- [ ] GitHub Actions role created
- [ ] Secrets added to repository
- [ ] Workflow tested successfully

### Android App ✅
- [ ] API endpoint configured
- [ ] APK built and installed
- [ ] Accessibility service enabled
- [ ] Microphone permission granted

### End-to-End ✅
- [ ] Can record audio
- [ ] Can send to API
- [ ] Transcription works
- [ ] Results stored in DynamoDB
- [ ] Audio stored in S3
- [ ] Logs visible in CloudWatch

---

## Troubleshooting Reference

### CloudFormation Stack Failed

```bash
# Check events
aws cloudformation describe-stack-events \
  --stack-name voice-mode-stack-dev \
  --query "StackEvents[0:10]"

# Check specific error
aws cloudformation describe-stacks \
  --stack-name voice-mode-stack-dev \
  --query "Stacks[0].StackStatusReason"
```

### Lambda Not Running

```bash
# Check function exists
aws lambda get-function --function-name voice-mode-transcribe-dev

# Check environment variables
aws lambda get-function-configuration \
  --function-name voice-mode-transcribe-dev

# Check execution role
aws iam get-role --role-name voice-mode-transcribe-dev
```

### API Gateway Not Responding

```bash
# Test without auth
curl -X POST https://xxxxx.execute-api.us-east-1.amazonaws.com/dev/transcribe

# Test with auth
curl -X POST https://xxxxx.execute-api.us-east-1.amazonaws.com/dev/transcribe \
  -H "Authorization: Bearer $VOICE_MODE_API_KEY"
```

### DynamoDB Issues

```bash
# Check table exists
aws dynamodb describe-table --table-name voice-mode-transcriptions-dev

# Check items
aws dynamodb scan --table-name voice-mode-transcriptions-dev
```

---

## Summary

**You now have:**
1. ✅ OpenAI API configured and tested
2. ✅ AWS account set up with IAM user
3. ✅ Voice Mode API key generated
4. ✅ AWS infrastructure deployed
5. ✅ GitHub OIDC configured
6. ✅ GitHub Actions secrets set up
7. ✅ Android app configured
8. ✅ End-to-end tested

**Next Steps:**
1. Monitor CloudWatch for errors
2. Track AWS costs
3. Plan scaling as usage grows
4. Consider production deployment

**Cost Tracking:**
- Check AWS billing: https://console.aws.amazon.com/billing/
- Monitor Lambda: CloudWatch → Metrics
- Track OpenAI: https://platform.openai.com/account/usage/overview

---

**Total Time**: ~2-3 hours
**Status**: 🚀 **READY FOR PRODUCTION**

