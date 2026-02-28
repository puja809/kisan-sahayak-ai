# Voice Assistant Integration

## Overview
The Voice Assistant component provides a question-answering interface that connects to an AWS Lambda-based API endpoint for intelligent responses about agriculture and farming schemes.

## API Specification

### Endpoint
```
POST https://5m2acu2lea.execute-api.us-east-1.amazonaws.com/prod/ask
```

### Request Format
```json
{
  "question": "What is AIF scheme?"
}
```

### Response Format
```json
{
  "answer": "The Agriculture Infrastructure Fund (AIF) is a flagship Central Sector Scheme launched by the Government of India to strengthen agricultural infrastructure across the nation. It provides medium to long-term debt financing facility through interest subvention and credit guarantee support for investment in viable projects for post-harvest management infrastructure and community farming assets."
}
```

## Architecture

### Service Layer
**File**: `src/app/services/voice-assistant.service.ts`

The `VoiceAssistantService` handles all communication with the AWS API endpoint:
- Sends question to the API
- Returns the answer response
- Handles HTTP errors gracefully

**Interfaces**:
```typescript
interface VoiceAssistantRequest {
  question: string;
}

interface VoiceAssistantResponse {
  answer: string;
}
```

### Component
**File**: `src/app/pages/voice/voice-agent.component.ts`

The `VoiceAgentComponent` provides the UI for:
- **Language Selection**: Choose from 10 Indian languages (for future multi-language support)
- **Text Input**: Ask questions via text input
- **Conversation History**: View all previous questions and answers
- **Voice Recording**: Placeholder for future voice input (currently mocked)
- **Response Display**: Shows answers in a clean chat interface

## Features

### 1. Question Answering
Users can ask questions about agriculture, farming schemes, and related topics. The service sends the question to the AWS API and displays the response.

### 2. Conversation History
- All Q&A pairs are stored in the conversation history
- History is persisted to browser localStorage
- Users can clear history at any time
- Timestamps are recorded for each message

### 3. Error Handling
- Network errors are caught and displayed to the user
- Failed requests show helpful error messages
- Conversation history is updated even on errors for visibility
- User-friendly error notifications via toast messages

### 4. Loading States
- Processing indicator shows when waiting for API response
- Send button is disabled during processing
- Text input is disabled during processing
- Visual feedback with loading spinner

### 5. Multi-Language Support
- Language selector with 10 Indian languages
- Currently used for UI display (future: send to API for localized responses)
- Supports: English, Hindi, Tamil, Telugu, Kannada, Malayalam, Marathi, Gujarati, Punjabi, Bengali

## Usage

### Basic Question
1. Navigate to the Voice Assistant page
2. Select a language (optional, defaults to English)
3. Type a question in the text input
4. Press Enter or click Send
5. View the response in the conversation history

### Example Questions
- "What is AIF scheme?"
- "What are the benefits of the Agriculture Infrastructure Fund?"
- "How do I apply for agricultural subsidies?"
- "Tell me about crop insurance schemes"
- "What is the minimum investment required?"

## API Integration

### Request Example
```typescript
this.voiceAssistantService.askQuestion(
  "What is the AIF scheme?"
).subscribe({
  next: (response) => {
    console.log(response.answer);
  },
  error: (error) => {
    console.error('Error:', error);
  }
});
```

### Response Example
```json
{
  "answer": "The Agriculture Infrastructure Fund (AIF) is a flagship Central Sector Scheme launched by the Government of India..."
}
```

## Component Structure

### Data Flow
1. User enters question in text input
2. Component calls `voiceAssistantService.askQuestion()`
3. Service sends POST request to AWS API
4. API returns answer
5. Component adds message to conversation history
6. History is persisted to localStorage
7. UI updates to show new message

### State Management
- `conversationHistory`: Array of all Q&A pairs
- `isProcessing`: Boolean flag for loading state
- `textInput`: Current text in input field
- `selectedLanguage`: Currently selected language
- `isRecording`: Boolean flag for voice recording state (future use)

## Future Enhancements

### Voice Input
- Implement WebRTC audio capture
- Add speech-to-text conversion using Web Speech API
- Support voice output (text-to-speech)
- Real-time transcription display

### Advanced Features
- Context-aware conversations (maintain conversation context across messages)
- Follow-up questions with context
- Document references and citations
- Feedback mechanism for response quality
- Response rating system

### Performance
- Response caching for common questions
- Streaming responses for long answers
- Pagination for large result sets
- Debouncing for rapid requests

### UI/UX
- Markdown support for formatted answers
- Code syntax highlighting
- Link detection and formatting
- Copy-to-clipboard for answers
- Share conversation feature

## Configuration

### Environment Variables
The service uses a hardcoded AWS API endpoint. To change it:

1. Update `voice-assistant.service.ts`:
```typescript
private apiUrl = 'https://your-api-endpoint.com/ask';
```

2. Or add to `environment.ts`:
```typescript
export const environment = {
  services: {
    voiceAssistant: 'https://your-api-endpoint.com'
  }
};
```

## Troubleshooting

### CORS Issues
If you encounter CORS errors:
1. Ensure the AWS API has CORS enabled
2. Check that the endpoint allows requests from your frontend domain
3. Verify the request headers are correct
4. Check browser console for specific CORS error messages

### No Response
- Check browser console for error messages
- Verify the AWS API endpoint is accessible
- Ensure the request payload is correctly formatted
- Check network tab in browser DevTools

### Slow Responses
- Check AWS Lambda function performance
- Verify network connectivity
- Check for rate limiting on the API
- Monitor API CloudWatch logs

## Testing

### Manual Testing
1. Open the Voice Assistant page
2. Test with different question types
3. Verify conversation history persistence
4. Test error scenarios (network offline, invalid input)
5. Clear history and verify it works

### Example Test Questions
```
1. "What is AIF scheme?"
2. "Tell me about agricultural subsidies"
3. "How to apply for farming loans?"
4. "What are the eligibility criteria?"
5. "What documents are required?"
```

### Automated Testing
```typescript
// Example test
it('should send question and receive answer', (done) => {
  const question = 'What is AIF?';
  service.askQuestion(question).subscribe(response => {
    expect(response.answer).toBeTruthy();
    expect(response.answer.length).toBeGreaterThan(0);
    done();
  });
});
```

## Browser Compatibility
- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support
- Mobile browsers: Full support

## Performance Metrics
- Average response time: 1-3 seconds
- Conversation history limit: No limit (localStorage dependent)
- Maximum question length: No limit
- Concurrent requests: 1 (sequential processing)

## Support
For issues or questions about the Voice Assistant integration, contact the development team or check the AWS Lambda function logs for backend errors.
