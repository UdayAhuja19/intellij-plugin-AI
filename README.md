# FocusFlow

FocusFlow is an intelligent coding assistant integrated directly into IntelliJ IDEA. It helps developers understand, improve, and document their code using AI.

## Features

### 🤖 FocusFlow Chat
Have interactive conversations with AI about your code directly within the IDE. The chat interface supports streaming responses for a fluid experience.

### 🧠 Code Assistance
- **Explain Code**: Get detailed explanations of selected code snippets.
- **Improve Code**: Receive suggestions for optimizing and refactoring your code.
- **Detect Errors**: Automatically find bugs and get hints or solutions.
- **What Am I Doing?**: Analyze your current code context to summarize your task.

### 📝 Content Generation
- **Generate Blog Post**: Create HTML blog posts documenting your selected code.
- **Generate LinkedIn Post**: Draft professional LinkedIn posts to share your coding achievements.

### ⚡ Quick Ask
Select code and press `Ctrl+Alt+A` (or your mapped shortcut) to quickly ask FocusFlow a question.

## Installation

### From Source
1.  **Prerequisites**:
    *   Java 17 or higher
    *   Validation of your environment for IntelliJ Plugin development.

2.  **Clone the repository**:
    ```bash
    git clone <repository-url>
    cd intellij-plugin-AI
    ```

3.  **Run the IDE**:
    This will compile the plugin and start a sandbox instance of IntelliJ IDEA with FocusFlow installed.
    ```bash
    ./gradlew runIde
    ```

## Usage

1.  **Open the Tool Window**: Click on the "FocusFlow" icon on the right sidebar or find it in `View > Tool Windows > FocusFlow`.
2.  **Context Menu**: Right-click on any code selection in the editor to access FocusFlow actions under the "FocusFlow" group.
3.  **Settings**: Configure your AI providers (OpenAI, Anthropic, or Custom) in `Settings/Preferences > Tools > FocusFlow`.

## Development

- **Build Plugin**:
    ```bash
    ./gradlew buildPlugin
    ```
    The artifact will be generated in `build/libs/`.

- **Run Tests**:
    ```bash
    ./gradlew test
    ```
