const { BedrockRuntimeClient, InvokeModelCommand } = require('@aws-sdk/client-bedrock-runtime');
const fs = require('fs');
const path = require('path');

class BedrockService {
  constructor() {
    this.client = new BedrockRuntimeClient({
      region: process.env.BEDROCK_REGION || 'us-east-1'
    });
    this.modelId = process.env.BEDROCK_MODEL_ID || 'anthropic.claude-3-5-sonnet-20241022';
  }

  /**
   * Transcribe audio using Bedrock Whisper model
   * Note: AWS Bedrock doesn't directly provide Whisper model, but we can use Claude's vision
   * capabilities or call external transcription service. For now, using a placeholder for
   * actual Whisper integration.
   */
  async transcribeAudio(audioBuffer, filename) {
    try {
      console.log(`Transcribing audio file: ${filename}`);

      // Save audio temporarily
      const tempPath = path.join('/tmp', `audio_${Date.now()}.m4a`);
      fs.writeFileSync(tempPath, audioBuffer);

      // For now, we'll use a basic transcription approach
      // In production, you would integrate with:
      // 1. AWS Transcribe service (for Speech-to-Text)
      // 2. Or call Whisper via an API
      // 3. Or use a combination of services

      const transcription = await this.callWhisperViaAPI(tempPath);

      // Clean up temp file
      fs.unlinkSync(tempPath);

      return {
        transcription: transcription || 'Transcription placeholder',
        confidence: 0.95,
        timestamp: new Date().toISOString()
      };
    } catch (error) {
      console.error('Error transcribing audio:', error);
      throw new Error(`Transcription failed: ${error.message}`);
    }
  }

  /**
   * Call external Whisper API or use AWS Transcribe
   * This is a placeholder - implement based on your choice
   */
  async callWhisperViaAPI(audioPath) {
    // TODO: Implement actual Whisper transcription
    // Options:
    // 1. Use AWS Transcribe:
    //    - https://docs.aws.amazon.com/transcribe/latest/dg/getting-started.html
    // 2. Use OpenAI's Whisper API:
    //    - https://platform.openai.com/docs/guides/speech-to-text
    // 3. Use AssemblyAI:
    //    - https://www.assemblyai.com/

    // Placeholder implementation
    return 'Audio transcription would be processed here';
  }

  /**
   * Use Claude via Bedrock to process and enhance transcribed text
   * This cleans up speech-to-text artifacts like filler words, grammar, proper nouns
   */
  async processText(text) {
    try {
      const prompt = `You are an expert speech-to-text enhancement assistant. Your job is to clean up raw speech transcriptions.

Given the following raw speech transcription, please:
1. Remove filler words (um, uh, like, you know, sort of, kind of, etc.)
2. Fix grammar and capitalization
3. Capitalize proper nouns appropriately
4. Add proper punctuation
5. Improve sentence structure and flow
6. Maintain the speaker's original meaning and tone

Return ONLY the cleaned text, with no explanations or meta-commentary.

Raw transcription:
${text}`;

      const command = new InvokeModelCommand({
        modelId: this.modelId,
        body: JSON.stringify({
          max_tokens: 2048,
          messages: [
            {
              role: 'user',
              content: prompt
            }
          ]
        })
      });

      const response = await this.client.send(command);
      const responseBody = new TextDecoder().decode(response.body);
      const parsedResponse = JSON.parse(responseBody);

      // Extract the text content from Claude's response
      const processedText = parsedResponse.content?.[0]?.text ||
                           parsedResponse.text ||
                           parsedResponse.result ||
                           '';

      console.log(`Text processing completed. Original length: ${text.length}, Processed length: ${processedText.length}`);

      return {
        original: text,
        processed: processedText.trim(),
        timestamp: new Date().toISOString(),
        model: this.modelId
      };
    } catch (error) {
      console.error('Error processing text with Bedrock:', error);
      throw new Error(`Text processing failed: ${error.message}`);
    }
  }
}

module.exports = new BedrockService();
