const express = require('express');
const router = express.Router();
const { verifyApiKey } = require('../middleware/auth');
const bedrockService = require('../services/bedrockService');

/**
 * POST /api/transcribe
 * Transcribe audio using Bedrock Whisper
 *
 * Request body:
 * {
 *   "audio": "base64-encoded-audio-data",
 *   "filename": "recording.m4a"
 * }
 */
router.post('/transcribe', verifyApiKey, async (req, res) => {
  try {
    const { audio, filename } = req.body;

    if (!audio || !filename) {
      return res.status(400).json({
        error: 'Missing required fields: audio, filename'
      });
    }

    // Decode base64 audio
    const audioBuffer = Buffer.from(audio, 'base64');

    // Transcribe using Bedrock
    const result = await bedrockService.transcribeAudio(audioBuffer, filename);

    res.json({
      success: true,
      transcription: result.transcription,
      confidence: result.confidence,
      timestamp: result.timestamp
    });
  } catch (error) {
    console.error('Transcription error:', error);
    res.status(500).json({
      error: 'Transcription failed',
      message: error.message
    });
  }
});

/**
 * POST /api/process-text
 * Process transcribed text with Claude via Bedrock
 *
 * Request body:
 * {
 *   "text": "the text to process"
 * }
 */
router.post('/process-text', verifyApiKey, async (req, res) => {
  try {
    const { text } = req.body;

    if (!text) {
      return res.status(400).json({
        error: 'Missing required field: text'
      });
    }

    const result = await bedrockService.processText(text);

    res.json({
      success: true,
      result: result
    });
  } catch (error) {
    console.error('Text processing error:', error);
    res.status(500).json({
      error: 'Text processing failed',
      message: error.message
    });
  }
});

module.exports = router;
