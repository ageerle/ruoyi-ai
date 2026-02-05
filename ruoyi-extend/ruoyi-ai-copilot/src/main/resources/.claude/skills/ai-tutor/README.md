# ai-tutor-skill

A Claude skill for explaining complex AI and ML concepts in accessible, plain English. This skill transforms abstract technical ideas into clear explanations using structured narrative frameworks, making it ideal for teaching and learning technical topics.

Resources:
- [YouTube Explainer](https://youtu.be/vEvytl7wrGM)

## Requirements

- **Python**: >= 3.12
- **Package Manager**: [uv](https://github.com/astral-sh/uv)
- **Dependencies**: youtube-transcript-api (installed automatically)

## Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd ai-tutor-skill
   ```

2. Install dependencies:
   ```bash
   uv sync
   ```

## Important Note: YouTube Transcript Limitations

> **The YouTube transcript functionality only works when Claude Code is running locally.**
>
> YouTube blocks requests from Claude's servers, so transcript extraction will fail when using Claude Code in cloud/remote mode. To use this feature, ensure you're running Claude Code on your local machine.
