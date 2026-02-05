# Research Methodology for Explaining Unfamiliar Concepts

Use this guide when you encounter concepts outside your reliable knowledge or when explaining cutting-edge developments.

## When to Research

**Always research when:**
- Concept is unfamiliar or outside your training data
- Topic involves developments after early 2025
- User references specific papers, articles, or sources
- Explaining cutting-edge techniques or recent breakthroughs
- You're uncertain about technical accuracy
- User asks "what's new" or "recent developments"

**Don't research when:**
- Explaining well-established, fundamental concepts (e.g., gradient descent, neural networks)
- You have high confidence in your knowledge
- Topic is clearly pre-2025 and stable

## Research Strategy

### 1. Start with Broad Context (web_search)

**Effective search queries:**
- For new concepts: `"{concept name}" explained tutorial`
- For recent developments: `"{concept}" 2024 2025 latest`
- For comparisons: `"{concept A}" vs "{concept B}" differences`
- For practical use: `"{concept}" real world applications examples`

**Evaluate search results:**
- Prioritize: Official documentation, academic institutions, reputable tech blogs
- Look for: Recent dates, author credentials, technical depth
- Avoid: Marketing content, SEO spam, unsourced claims

**Extract from results:**
- Core definition in plain language
- Key motivations (what problem it solves)
- Main components or mechanisms
- Concrete examples or applications
- Common misconceptions

### 2. Deep Dive on Best Sources (web_fetch)

**When to fetch full content:**
- Found a particularly clear explanation
- Need technical details for accuracy
- Source is academic paper or official documentation
- Initial search didn't provide sufficient depth

**What to extract from full articles:**
- The author's own plain English summary (often in intro/conclusion)
- Concrete examples with specific numbers or data
- Diagrams or visual explanations (note what they show)
- Comparison to previous/alternative approaches
- Practical applications or use cases

**Reading academic papers:**
- Start with abstract and conclusion
- Look for "In this paper, we..." statements for plain English summary
- Check "Related Work" section to understand context
- Extract key innovation/contribution in one sentence
- Find any "intuition" or "motivation" sections

### 3. Synthesize Multiple Sources

**When sources agree:**
- Use the clearest explanation as your base
- Incorporate best concrete examples from various sources
- Combine different perspectives for completeness

**When sources conflict:**
- Identify what they disagree about
- Look for authoritative sources (original papers, official docs)
- Note the disagreement in your explanation if significant
- Don't hide uncertainty - acknowledge different perspectives

**Red flags to watch for:**
- Single source makes claims not found elsewhere
- Marketing language disguised as technical explanation
- Overly simplified analogies that mislead
- Cherry-picked benchmarks or examples

### 4. Extract from YouTube Videos

**When to use YouTube transcripts:**
- User directly references a video
- Video is from reputable educator/researcher
- Need concrete examples from tutorial content
- Want to see how concept is explained to learners

**Extracting from transcripts:**
```bash
# Basic usage - returns full transcript
uv run scripts/get_youtube_transcript.py <video_url_or_id>

# With timestamps for reference
uv run scripts/get_youtube_transcript.py <video_url_or_id> --timestamps

# Supports multiple URL formats
uv run scripts/get_youtube_transcript.py dQw4w9WgXcQ
uv run scripts/get_youtube_transcript.py https://www.youtube.com/watch?v=dQw4w9WgXcQ
uv run scripts/get_youtube_transcript.py https://youtu.be/dQw4w9WgXcQ
```

**What to look for in transcripts:**
- The educator's own analogies (often well-tested)
- Concrete examples with walkthroughs
- Common student questions addressed
- Simpler explanations before technical ones
- Visual descriptions ("as you can see in this diagram...")

**Transcript limitations:**
- May include verbal fillers and repetition
- Missing visual context (slides, diagrams)
- Informal language may need translation to written form

## Source Quality Hierarchy

**Tier 1 (Highest Trust):**
- Original research papers from reputable venues
- Official documentation from source (e.g., OpenAI docs for GPT)
- University course materials
- Books from established publishers

**Tier 2 (High Trust):**
- Technical blogs from recognized experts
- Conference presentations and talks
- Reputable tech news sites (with technical depth)
- Well-maintained wikis with citations

**Tier 3 (Use with Caution):**
- Medium articles (verify author credentials)
- Stack Overflow (good for practical issues, not concepts)
- Reddit discussions (good for perspectives, not authority)
- Tutorial sites (verify accuracy against Tier 1/2 sources)

**Tier 4 (Avoid):**
- Marketing materials posing as education
- Uncited claims
- Sensationalized headlines
- Anonymous sources without verifiable expertise

## Handling Uncertainty

**When research reveals gaps:**
- Be explicit: "Based on the sources I found..." 
- Explain what you learned and what remains unclear
- Offer to research specific aspects more deeply
- Don't fill gaps with speculation

**When sources are insufficient:**
- State what you know with confidence
- Acknowledge limitations: "The available sources don't provide clear information on..."
- Suggest where user might find more detail
- Offer to continue researching if user wants

**When completely unfamiliar:**
- Don't hide it: "This is a cutting-edge concept I need to research"
- Do thorough research before explaining
- Synthesize from multiple high-quality sources
- Be clear about confidence level in your explanation

## Common Research Mistakes to Avoid

❌ **Relying on single source** - Always cross-reference
❌ **Using first search result** - Evaluate multiple sources
❌ **Ignoring publication date** - Recent developments need recent sources
❌ **Accepting marketing claims** - Verify with technical sources
❌ **Skipping paper abstracts** - Authors' own summaries are gold
❌ **Over-trusting tutorials** - Verify technical accuracy
❌ **Hiding uncertainty** - Better to acknowledge gaps

## Research Workflow Example

**User asks:** "Explain mixture of experts in LLMs"

**Step 1 - Quick assessment:**
- Topic: Recent development (2023-2024)
- Confidence: Medium (know concept but not latest implementations)
- Decision: Research needed

**Step 2 - Broad search:**
```
web_search: "mixture of experts LLMs 2024 explained"
```
- Find: Mixtral announcement, technical blog posts, comparisons
- Note: Different from traditional MoE in NLP
- Extract: Core idea, recent models using it, key benefits

**Step 3 - Deep dive:**
```
web_fetch: [Best technical blog or paper URL]
```
- Extract: Technical details, architecture specifics
- Find: Concrete comparison (Mixtral 8x7B vs GPT-3.5)
- Note: Load balancing, routing mechanisms

**Step 4 - Synthesize:**
- Core concept: Sparse activation of expert networks
- Problem it solves: Scaling without proportional compute increase
- How it works: Router selects subset of experts per token
- Example: Mixtral uses 8 experts, activates 2 per token
- Result: 47B parameters, 13B active per token

**Step 5 - Explain:**
Use Status Quo → Problem → Solution structure with researched content.

## Integration with Teaching Principles

After research, apply teaching framework:

1. **Choose narrative structure** based on concept nature
2. **Plain English first** - use clearest definition found
3. **Concrete examples** - use specific instances from research
4. **Strategic analogies** - adopt effective ones from sources
5. **Cite implicitly** - "Recent research shows..." not "According to source X..."

Research informs content; teaching principles guide delivery.
