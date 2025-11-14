# GitHub Actions CI/CD Setup

Automated deployment of Voice Mode to AWS using GitHub Actions.

## Prerequisites

1. GitHub repository with Voice Mode code
2. AWS Account with CLI access
3. OpenAI API key
4. Generated Voice Mode API key

## Step 1: Create AWS IAM Role for GitHub Actions

GitHub Actions uses OIDC (OpenID Connect) to securely authenticate with AWS without storing long-term credentials.

### 1.1 Create IAM Provider

```bash
# Create OIDC provider for GitHub
aws iam create-open-id-connect-provider \
  --url "https://token.actions.githubusercontent.com" \
  --client-id-list "sts.amazonaws.com" \
  --thumbprint-list "6938fd4d98bab03faadb97b34396831e3780aca1"
```

### 1.2 Create IAM Role

```bash
# Create trust policy file
cat > trust-policy.json << 'EOF'
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

# Replace ACCOUNT_ID with your AWS Account ID
# Replace GITHUB_USER with your GitHub username

# Create role
aws iam create-role \
  --role-name VoiceModeGitHubActionsRole \
  --assume-role-policy-document file://trust-policy.json
```

### 1.3 Add Policies

```bash
# Policy for CloudFormation
cat > cf-policy.json << 'EOF'
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
  --policy-document file://cf-policy.json

# Get role ARN
aws iam get-role \
  --role-name VoiceModeGitHubActionsRole \
  --query 'Role.Arn' \
  --output text
```

Save the role ARN - you'll need it in the next step.

## Step 2: Add GitHub Secrets

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add the following secrets:

| Secret Name | Value |
|------------|-------|
| `AWS_ROLE_ARN` | ARN from Step 1.3 |
| `OPENAI_API_KEY` | Your OpenAI API key (`sk-...`) |
| `VOICE_MODE_API_KEY` | Generated API key (32-character hex string) |

### Example

```
AWS_ROLE_ARN=arn:aws:iam::123456789012:role/VoiceModeGitHubActionsRole
OPENAI_API_KEY=sk-abc123def456...
VOICE_MODE_API_KEY=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
```

## Step 3: Configure Workflow

The workflow file is already created at `.github/workflows/deploy.yml`.

Review the configuration:

```yaml
on:
  push:
    branches:
      - main
    paths:
      - 'aws-lambda/**'
      - 'aws-infrastructure/**'
      - '.github/workflows/deploy.yml'
  workflow_dispatch:
    inputs:
      environment:
        default: 'dev'
        type: choice
        options:
          - dev
          - staging
          - prod
```

This means:
- **Automatic**: Deploy when you push changes to `aws-lambda/` or `aws-infrastructure/`
- **Manual**: Trigger deployment from GitHub UI with environment selection

## Step 4: Test the Workflow

### Option 1: Automatic Trigger

```bash
# Make a change to trigger automatic deployment
echo "# Test" >> aws-infrastructure/README.md
git add .
git commit -m "Test GitHub Actions deployment"
git push origin main
```

### Option 2: Manual Trigger

1. Go to GitHub repository
2. Click **Actions** tab
3. Click **Deploy to AWS** workflow
4. Click **Run workflow** button
5. Select environment (dev/staging/prod)
6. Click **Run workflow**

### Monitor Deployment

1. Go to **Actions** tab
2. Click the workflow run
3. Click **deploy** job
4. Watch logs in real-time

## Workflow Stages

### Build

```yaml
- name: Build Lambda function
  run: |
    cd aws-lambda/transcribe
    npm install --production
    zip -r ../../function.zip .
```

Packages the Lambda function and dependencies into a ZIP file.

### Deploy

```yaml
- name: Deploy CloudFormation stack
  run: |
    # Checks if stack exists
    # Creates new or updates existing
    # Applies CloudFormation template
```

Creates or updates CloudFormation stack with all AWS resources.

### Wait

```yaml
- name: Wait for stack
  run: |
    aws cloudformation wait stack-create-complete
```

Waits for CloudFormation to complete (usually 2-5 minutes).

### Update Lambda

```yaml
- name: Update Lambda function code
  run: |
    # Updates Lambda function with new code
```

Uploads the packaged Lambda function to AWS.

### Test

```yaml
- name: Test API endpoint
  run: |
    # Tests API health
```

Optional test of the deployed API endpoint.

## Environment-Specific Configuration

### Development (dev)

- Deployed automatically on push
- No approval needed
- Development databases and storage

### Staging

```bash
# Manual trigger to staging
# GitHub UI → Actions → Deploy to AWS → Run workflow → staging
```

- Manual deployment
- Staging databases
- Full testing environment

### Production (prod)

```bash
# Manual trigger to production
# GitHub UI → Actions → Deploy to AWS → Run workflow → prod
```

- Manual deployment
- Production databases
- Live environment
- **Requires careful testing first**

Each environment has its own:
- CloudFormation stack
- Lambda function
- DynamoDB table
- S3 bucket
- API Gateway endpoint

## Troubleshooting

### Workflow Fails to Authenticate

**Error**: `Error: Not authorized to perform: sts:AssumeRoleWithWebIdentity`

**Solution**:
1. Check AWS_ROLE_ARN secret is correct
2. Verify GitHub username in trust policy
3. Verify OIDC provider is created in AWS

### CloudFormation Fails

**Error**: `ResourceInUseException: Resource of type 'AWS::Lambda::Function' with identifier ... already exists`

**Solution**:
1. Check stack name is correct
2. Delete old stack manually if corrupted:
   ```bash
   aws cloudformation delete-stack --stack-name voice-mode-stack-dev
   ```

### Lambda Code Not Updated

**Error**: Lambda function not using latest code

**Solution**:
1. Check `function.zip` is properly created in build step
2. Verify Lambda function has permission to assume IAM role
3. Check CloudWatch logs for any errors

## Monitoring Deployments

### View Workflow Runs

```bash
# Using GitHub CLI
gh run list --repo username/voice-mode

# Watch specific run
gh run watch RUN_ID --repo username/voice-mode
```

### View CloudFormation Events

```bash
aws cloudformation describe-stack-events \
  --stack-name voice-mode-stack-dev \
  --region us-east-1
```

### Check Lambda Logs

```bash
aws logs tail /aws/lambda/voice-mode-transcribe-dev --follow
```

## Best Practices

1. **Test in dev first** - Always deploy to dev before staging/prod
2. **Review changes** - Check what's being deployed in workflow logs
3. **Tag releases** - Use Git tags for production deployments
4. **Monitor costs** - Check AWS billing after each deployment
5. **Backup state** - Keep CloudFormation stack intact, don't delete manually

## Advanced Configuration

### Slack Notifications

Add this step to get Slack notifications on deployment:

```yaml
- name: Notify Slack
  if: always()
  uses: slackapi/slack-github-action@v1.24.0
  with:
    payload: |
      {
        "text": "Voice Mode deployment ${{ job.status }}",
        "channel": "deployments"
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### Approval Gates

For production:

```yaml
environment:
  name: prod
  deployment_branch_policy:
    protected_branches: true
    custom_deployment_branch_policies: false
```

### Scheduled Deployments

Deploy on schedule:

```yaml
schedule:
  - cron: '0 2 * * MON'  # Every Monday at 2 AM UTC
```

## Cleanup

### Delete a Stack

```bash
aws cloudformation delete-stack --stack-name voice-mode-stack-dev
aws cloudformation wait stack-delete-complete --stack-name voice-mode-stack-dev
```

### Delete OIDC Provider

```bash
aws iam delete-open-id-connect-provider \
  --open-id-connect-provider-arn arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com
```

## References

- [GitHub Actions AWS Documentation](https://github.com/aws-actions/configure-aws-credentials)
- [AWS CloudFormation Documentation](https://docs.aws.amazon.com/cloudformation/)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

## Support

For issues:
1. Check workflow logs in GitHub Actions
2. Review CloudFormation events in AWS Console
3. Check CloudWatch logs for Lambda errors
4. Verify all secrets are set correctly
