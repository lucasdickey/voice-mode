/**
 * AWS Lambda function for transcribing audio using OpenAI Whisper API
 * Triggered by: API Gateway POST /transcribe
 * Environment variables required:
 * - OPENAI_API_KEY: OpenAI API key
 * - DYNAMODB_TABLE: DynamoDB table name
 * - S3_BUCKET: S3 bucket for audio storage
 */

const AWS = require('aws-sdk');
const https = require('https');
const { v4: uuidv4 } = require('uuid');
const FormData = require('form-data');
const Readable = require('stream').Readable;

const s3 = new AWS.S3();
const dynamodb = new AWS.DynamoDB.DocumentClient();

const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const DYNAMODB_TABLE = process.env.DYNAMODB_TABLE;
const S3_BUCKET = process.env.S3_BUCKET;

/**
 * Call OpenAI Whisper API to transcribe audio
 */
async function callWhisperAPI(audioBuffer, filename) {
  return new Promise((resolve, reject) => {
    const form = new FormData();

    // Add audio file
    const stream = new Readable();
    stream.push(audioBuffer);
    stream.push(null);

    form.append('file', stream, filename);
    form.append('model', 'whisper-1');
    form.append('language', 'en');

    const options = {
      hostname: 'api.openai.com',
      path: '/v1/audio/transcriptions',
      method: 'POST',
      headers: {
        ...form.getHeaders(),
        'Authorization': `Bearer ${OPENAI_API_KEY}`
      }
    };

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          try {
            const result = JSON.parse(data);
            resolve({
              transcription: result.text,
              confidence: 0.95,
              language: result.language || 'en'
            });
          } catch (e) {
            reject(new Error(`Failed to parse Whisper response: ${e.message}`));
          }
        } else {
          reject(new Error(`Whisper API error: ${res.statusCode} - ${data}`));
        }
      });
    });

    req.on('error', reject);
    form.pipe(req);
  });
}

/**
 * Store transcription in DynamoDB
 */
async function storeTranscription(userId, audioS3Key, transcription, metadata) {
  const transcriptionId = uuidv4();
  const timestamp = new Date().toISOString();

  const params = {
    TableName: DYNAMODB_TABLE,
    Item: {
      transcriptionId,
      userId,
      timestamp,
      audioS3Key,
      transcription,
      confidence: metadata.confidence,
      language: metadata.language,
      duration: metadata.duration,
      fileSize: metadata.fileSize,
      ttl: Math.floor(Date.now() / 1000) + (90 * 24 * 60 * 60) // 90 days TTL
    }
  };

  await dynamodb.put(params).promise();
  return transcriptionId;
}

/**
 * Upload audio to S3
 */
async function uploadAudioToS3(audioBuffer, userId, filename) {
  const key = `audio/${userId}/${Date.now()}-${filename}`;

  const params = {
    Bucket: S3_BUCKET,
    Key: key,
    Body: audioBuffer,
    ContentType: 'audio/m4a',
    ServerSideEncryption: 'AES256',
    Metadata: {
      'user-id': userId,
      'original-filename': filename
    }
  };

  await s3.putObject(params).promise();
  return key;
}

/**
 * Main Lambda handler
 */
exports.handler = async (event) => {
  console.log('Transcription request received:', {
    headers: event.headers,
    body_length: event.body ? event.body.length : 0
  });

  try {
    // Parse request
    if (!event.body) {
      return {
        statusCode: 400,
        body: JSON.stringify({ error: 'Missing request body' }),
        headers: { 'Content-Type': 'application/json' }
      };
    }

    const body = JSON.parse(event.body);
    const { audio, filename, userId } = body;

    if (!audio || !filename) {
      return {
        statusCode: 400,
        body: JSON.stringify({ error: 'Missing audio or filename' }),
        headers: { 'Content-Type': 'application/json' }
      };
    }

    // Generate userId if not provided (use cognito sub in production)
    const actualUserId = userId || 'anonymous';

    // Decode base64 audio
    const audioBuffer = Buffer.from(audio, 'base64');
    console.log(`Decoded audio: ${audioBuffer.length} bytes`);

    // Upload to S3
    console.log('Uploading audio to S3...');
    const s3Key = await uploadAudioToS3(audioBuffer, actualUserId, filename);
    console.log(`Audio uploaded to S3: ${s3Key}`);

    // Call Whisper API
    console.log('Calling OpenAI Whisper API...');
    const transcriptionResult = await callWhisperAPI(audioBuffer, filename);
    console.log(`Transcription received: ${transcriptionResult.transcription.substring(0, 100)}...`);

    // Store in DynamoDB
    console.log('Storing transcription in DynamoDB...');
    const transcriptionId = await storeTranscription(
      actualUserId,
      s3Key,
      transcriptionResult.transcription,
      {
        confidence: transcriptionResult.confidence,
        language: transcriptionResult.language,
        duration: audioBuffer.length, // Rough estimate
        fileSize: audioBuffer.length
      }
    );
    console.log(`Stored with ID: ${transcriptionId}`);

    // Return success response
    return {
      statusCode: 200,
      body: JSON.stringify({
        success: true,
        transcription: transcriptionResult.transcription,
        confidence: transcriptionResult.confidence,
        transcriptionId,
        s3Key
      }),
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      }
    };

  } catch (error) {
    console.error('Error processing transcription:', error);
    return {
      statusCode: 500,
      body: JSON.stringify({
        error: 'Transcription failed',
        message: error.message
      }),
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      }
    };
  }
};
