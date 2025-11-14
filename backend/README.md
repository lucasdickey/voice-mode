# Voice Mode Backend Server

Express.js backend server for the Voice Mode Android app with AWS Bedrock integration for audio transcription using Whisper.

## Prerequisites

- Node.js 18+ and npm
- AWS Account with Bedrock access
- AWS credentials configured (via AWS CLI or environment variables)

## Setup

### 1. Install Dependencies

```bash
cd backend
npm install
```

### 2. Configure Environment

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

Required environment variables:
- `AWS_REGION`: AWS region for Bedrock (default: us-east-1)
- `AWS_ACCESS_KEY_ID`: Your AWS access key
- `AWS_SECRET_ACCESS_KEY`: Your AWS secret key
- `API_KEY`: Secure API key for client authentication
- `PORT`: Server port (default: 3000)

### 3. Configure AWS Bedrock

#### Option 1: AWS Transcribe (Recommended for Speech-to-Text)

1. Enable AWS Transcribe in your AWS account
2. Update the `bedrockService.js` to use AWS Transcribe client
3. Grant IAM permissions: `transcribe:StartTranscriptionJob`, `transcribe:GetTranscriptionJob`

#### Option 2: OpenAI Whisper API

1. Get API key from https://platform.openai.com
2. Update `bedrockService.js` to use OpenAI client
3. Add `openai` dependency: `npm install openai`

#### Option 3: AssemblyAI

1. Get API key from https://www.assemblyai.com
2. Update `bedrockService.js` to use AssemblyAI client
3. Add `assemblyai` dependency: `npm install assemblyai`

### 4. Start Development Server

```bash
npm run dev
```

Server will start on `http://localhost:3000`

### 5. Verify Health

```bash
curl http://localhost:3000/health
```

## API Endpoints

### Health Check
```
GET /health
```

Returns server status and uptime.

### Transcribe Audio
```
POST /api/transcribe
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json

{
  "audio": "base64-encoded-audio-data",
  "filename": "recording.m4a"
}
```

Response:
```json
{
  "success": true,
  "transcription": "Your transcribed text here",
  "confidence": 0.95,
  "timestamp": "2024-11-13T12:34:56.789Z"
}
```

### Process Text with Claude
```
POST /api/process-text
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json

{
  "text": "Text to process with Claude"
}
```

## Android App Configuration

Update the Android app with:
1. Backend API endpoint in `VoiceModeAccessibilityService.kt`
2. API key for authentication
3. Ensure INTERNET permission is granted

## Security Considerations

1. **API Keys**: Store securely in environment variables, never commit to git
2. **AWS Credentials**: Use IAM roles when deployed to AWS
3. **CORS**: Configure appropriately for your frontend domains
4. **Rate Limiting**: Add rate limiting middleware for production
5. **Input Validation**: All inputs are validated before processing

## Deployment

### Docker

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY src ./src
ENV NODE_ENV=production
EXPOSE 3000
CMD ["npm", "start"]
```

### AWS Lambda

Can be wrapped with Serverless Framework:
```bash
npm install -g serverless
serverless plugin install -n serverless-offline
```

### Docker Compose for Local Development

```yaml
version: '3.8'
services:
  backend:
    build: .
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=development
      - AWS_REGION=us-east-1
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
      - API_KEY=${API_KEY}
    volumes:
      - ./src:/app/src
```

## Testing

```bash
npm test
```

## Troubleshooting

### Missing AWS Credentials
```
Error: Missing credentials in configuration
```
Solution: Ensure AWS credentials are set in environment variables or ~/.aws/credentials

### Model Not Available
```
Error: Model ID not found in region
```
Solution: Verify your AWS region has access to the specified Bedrock model

### Audio Transcription Failing
- Check audio format is supported
- Verify audio file size (usually max 100MB)
- Ensure sufficient IAM permissions for transcription service

## Next Steps

1. Implement actual Whisper transcription (currently placeholder)
2. Add rate limiting and request validation
3. Add database for storing transcription history
4. Implement user authentication
5. Add monitoring and logging
6. Set up CI/CD pipeline
