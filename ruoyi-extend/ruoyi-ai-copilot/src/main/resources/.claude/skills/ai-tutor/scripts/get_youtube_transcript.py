#!/usr/bin/env python3
"""
YouTube Transcript Extractor

Extracts transcripts from YouTube videos using video IDs or URLs.
"""

import sys
import re
from youtube_transcript_api import YouTubeTranscriptApi
from youtube_transcript_api._errors import TranscriptsDisabled, NoTranscriptFound


def extract_video_id(url_or_id):
    """
    Extract YouTube video ID from various URL formats or return as-is if already an ID.
    
    Supports:
    - https://www.youtube.com/watch?v=VIDEO_ID
    - https://youtu.be/VIDEO_ID
    - VIDEO_ID (direct ID)
    """
    # Pattern for YouTube URLs
    patterns = [
        r'(?:youtube\.com\/watch\?v=)([a-zA-Z0-9_-]{11})',
        r'(?:youtu\.be\/)([a-zA-Z0-9_-]{11})',
        r'^([a-zA-Z0-9_-]{11})$'  # Direct video ID
    ]
    
    for pattern in patterns:
        match = re.search(pattern, url_or_id)
        if match:
            return match.group(1)
    
    return None


def get_transcript(video_id, language='en'):
    """
    Retrieve transcript for a YouTube video.
    
    Args:
        video_id: YouTube video ID
        language: Language code (default: 'en')
    
    Returns:
        List of transcript entries with 'text', 'start', and 'duration'
    """
    try:
        api = YouTubeTranscriptApi()
        transcript = api.fetch(video_id, languages=[language])
        return transcript
    except TranscriptsDisabled:
        raise Exception(f"Transcripts are disabled for video: {video_id}")
    except NoTranscriptFound:
        raise Exception(f"No transcript found for video: {video_id}")
    except Exception as e:
        raise Exception(f"Error fetching transcript: {str(e)}")


def format_transcript(transcript, include_timestamps=False):
    """
    Format transcript entries into readable text.
    
    Args:
        transcript: List of transcript entries
        include_timestamps: Whether to include timestamps
    
    Returns:
        Formatted transcript text
    """
    if include_timestamps:
        formatted = []
        for entry in transcript:
            minutes = int(entry.start // 60)
            seconds = int(entry.start % 60)
            formatted.append(f"[{minutes}:{seconds:02d}] {entry.text}")
        return '\n'.join(formatted)
    else:
        return ' '.join([entry.text for entry in transcript])


def main():
    """Main execution function"""
    if len(sys.argv) < 2:
        print("Usage: python get_youtube_transcript.py <video_url_or_id> [--timestamps]")
        print("\nExamples:")
        print("  uv run get_youtube_transcript.py dQw4w9WgXcQ")
        print("  uv run get_youtube_transcript.py https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        print("  uv run get_youtube_transcript.py dQw4w9WgXcQ --timestamps")
        sys.exit(1)
    
    url_or_id = sys.argv[1]
    include_timestamps = '--timestamps' in sys.argv
    
    # Extract video ID
    video_id = extract_video_id(url_or_id)
    if not video_id:
        print(f"Error: Could not extract video ID from: {url_or_id}")
        sys.exit(1)
    
    try:
        # Get transcript
        transcript = get_transcript(video_id)
        
        # Format and print
        formatted_text = format_transcript(transcript, include_timestamps)
        print(formatted_text)
        
    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
