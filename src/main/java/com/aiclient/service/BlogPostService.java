package com.aiclient.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating blog posts from code.
 * Creates HTML/CSS/JS documentation in the project's ai-blog-posts folder.
 */
public class BlogPostService {
    
    private static final String BLOG_FOLDER = "blog-posts";
    private static final String ASSETS_FOLDER = "assets";
    
    private final Project project;
    
    public BlogPostService(Project project) {
        this.project = project;
    }
    
    /**
     * Generates a blog post from the AI response and saves it to the project.
     * 
     * @param title The blog post title
     * @param content The AI-generated content (markdown-ish format)
     * @param originalCode The original code being documented
     * @param language The programming language of the code
     * @return Path to the generated HTML file
     */
    public String generateBlogPost(String title, String content, String originalCode, String language) throws IOException {
        // Create folder structure
        Path blogRoot = ensureBlogFolder();
        ensureAssets(blogRoot);
        
        // Create post folder with date prefix
        String slug = createSlug(title);
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String postFolderName = datePrefix + "-" + slug;
        Path postFolder = blogRoot.resolve(postFolderName);
        Files.createDirectories(postFolder);
        
        // Generate HTML
        String html = generateHtml(title, content, originalCode, language);
        
        // Write to file
        Path indexPath = postFolder.resolve("index.html");
        try (FileWriter writer = new FileWriter(indexPath.toFile())) {
            writer.write(html);
        }
        
        // Update index page
        updateIndexPage(blogRoot);
        
        // Refresh VFS
        LocalFileSystem.getInstance().refresh(true);
        
        return indexPath.toString();
    }
    
    private Path ensureBlogFolder() throws IOException {
        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new IOException("Project base path not found");
        }
        Path blogRoot = Path.of(basePath, BLOG_FOLDER);
        Files.createDirectories(blogRoot);
        return blogRoot;
    }
    
    private void ensureAssets(Path blogRoot) throws IOException {
        Path assetsPath = blogRoot.resolve(ASSETS_FOLDER);
        Files.createDirectories(assetsPath);
        
        // Create styles.css
        Path stylesPath = assetsPath.resolve("styles.css");
        if (!Files.exists(stylesPath)) {
            try (FileWriter writer = new FileWriter(stylesPath.toFile())) {
                writer.write(getStylesCss());
            }
        }
        
        // Create script.js
        Path scriptPath = assetsPath.resolve("script.js");
        if (!Files.exists(scriptPath)) {
            try (FileWriter writer = new FileWriter(scriptPath.toFile())) {
                writer.write(getScriptJs());
            }
        }
    }
    
    private String createSlug(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
    
    private String generateHtml(String title, String content, String originalCode, String language) {
        // Parse content sections
        String summary = extractSection(content, "SUMMARY", "KEY FEATURES");
        // Fix: Stop Key Features at WALKTHROUGH, not CODE
        String keyFeatures = extractSection(content, "KEY FEATURES", "WALKTHROUGH");
        String walkthrough = extractSection(content, "WALKTHROUGH", "IMPACT");
        String impact = extractSection(content, "IMPACT", null);
        
        // If parsing failed, use full content
        if (summary.isEmpty() && keyFeatures.isEmpty()) {
            summary = content;
        }
        
        return """
<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>%s</title>
    <link rel="stylesheet" href="../assets/styles.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css">
</head>
<body>
    <div class="container">
        <header>
            <button class="theme-toggle" onclick="toggleTheme()">🌙</button>
            <h1>%s</h1>
            <p class="date">Created on %s</p>
        </header>
        
        <main>
            <section class="summary">
                <h2>📋 Summary</h2>
                <p>%s</p>
            </section>
            
            %s
            
            <section class="code-section">
                <h2>💻 The Code</h2>
                <div class="code-container">
                    <button class="copy-btn" onclick="copyCode()">📋 Copy</button>
                    <pre><code class="language-%s">%s</code></pre>
                </div>
            </section>
            
            %s
            
            %s
        </main>
    </div>
    
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-%s.min.js"></script>
    <script src="../assets/script.js"></script>
</body>
</html>
""".formatted(
            escapeHtml(title),
            escapeHtml(title),
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            formatContent(summary),
            keyFeatures.isEmpty() ? "" : "<section class=\"features\"><h2>🔑 Key Features</h2>" + formatAsList(keyFeatures) + "</section>",
            language.isEmpty() ? "javascript" : language,
            escapeHtml(originalCode),
            walkthrough.isEmpty() ? "" : "<section class=\"walkthrough\"><h2>📖 Code Walkthrough</h2><p>" + formatContent(walkthrough) + "</p></section>",
            impact.isEmpty() ? "" : "<section class=\"impact\"><h2>🎯 Impact & Use Cases</h2><p>" + formatContent(impact) + "</p></section>",
            language.isEmpty() ? "javascript" : language
        );
    }
    
    private String extractSection(String content, String startMarker, String endMarker) {
        String upper = content.toUpperCase();
        int start = upper.indexOf(startMarker);
        if (start == -1) return "";
        
        start = content.indexOf("\n", start);
        if (start == -1) start = content.indexOf(startMarker) + startMarker.length();
        
        int end = endMarker != null ? upper.indexOf(endMarker) : content.length();
        if (end == -1 || end <= start) end = content.length();
        
        return content.substring(start, end).trim();
    }
    
    private String formatContent(String text) {
        if (text == null || text.isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");
        boolean inList = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip empty lines
            if (trimmed.isEmpty()) {
                if (inList) {
                    result.append("</ul>");
                    inList = false;
                }
                result.append("<br><br>");
                continue;
            }
            
            // Handle headers
            if (trimmed.startsWith("###")) {
                if (inList) { result.append("</ul>"); inList = false; }
                result.append("<h4>").append(formatInline(trimmed.substring(3).trim())).append("</h4>");
                continue;
            }
            if (trimmed.startsWith("##")) {
                if (inList) { result.append("</ul>"); inList = false; }
                result.append("<h3>").append(formatInline(trimmed.substring(2).trim())).append("</h3>");
                continue;
            }
            if (trimmed.startsWith("#")) {
                if (inList) { result.append("</ul>"); inList = false; }
                result.append("<h3>").append(formatInline(trimmed.substring(1).trim())).append("</h3>");
                continue;
            }
            
            // Handle list items
            if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")) {
                if (!inList) {
                    result.append("<ul>");
                    inList = true;
                }
                String content = trimmed.substring(1).trim();
                // Skip empty list items
                if (content.isEmpty()) continue;
                
                // Handle numbered sub-items like "1." or "2."
                if (content.matches("^\\d+\\..*")) {
                    content = content.replaceFirst("^\\d+\\.\\s*", "");
                }
                result.append("<li>").append(formatInline(content)).append("</li>");
                continue;
            }
            
            // Handle numbered lists
            if (trimmed.matches("^\\d+\\..*")) {
                if (!inList) {
                    result.append("<ul>");
                    inList = true;
                }
                String content = trimmed.replaceFirst("^\\d+\\.\\s*", "");
                result.append("<li>").append(formatInline(content)).append("</li>");
                continue;
            }
            
            // Regular paragraph text
            if (inList) {
                result.append("</ul>");
                inList = false;
            }
            result.append(formatInline(trimmed)).append(" ");
        }
        
        if (inList) {
            result.append("</ul>");
        }
        
        return result.toString();
    }
    
    /**
     * Format inline markdown: bold, italic, code, links
     */
    private String formatInline(String text) {
        if (text == null) return "";
        
        // Escape HTML first
        text = escapeHtml(text);
        
        // Bold: **text** or __text__
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("__(.+?)__", "<strong>$1</strong>");
        
        // Italic: *text* or _text_ (but not inside words)
        text = text.replaceAll("(?<![\\w*])\\*([^*]+)\\*(?![\\w*])", "<em>$1</em>");
        text = text.replaceAll("(?<![\\w_])_([^_]+)_(?![\\w_])", "<em>$1</em>");
        
        // Inline code: `code`
        text = text.replaceAll("`([^`]+)`", "<code>$1</code>");
        
        return text;
    }
    
    private String formatAsList(String text) {
        StringBuilder sb = new StringBuilder("<ul>");
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            
            // Skip header lines (##, #, etc.)
            if (trimmed.startsWith("#")) {
                continue;
            }
            
            // Skip empty lines
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Remove list markers
            if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*") && !trimmed.startsWith("**")) {
                trimmed = trimmed.substring(1).trim();
            }
            // Handle ** at start (bold marker, not list)
            if (trimmed.startsWith("**")) {
                // It's a bold item, not a list marker - leave for formatInline to handle
            }
            // Remove numbered list markers
            trimmed = trimmed.replaceFirst("^\\d+\\.\\s*", "");
            
            if (!trimmed.isEmpty()) {
                sb.append("<li>").append(formatInline(trimmed)).append("</li>");
            }
        }
        sb.append("</ul>");
        return sb.toString();
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
    
    private void updateIndexPage(Path blogRoot) throws IOException {
        List<String> posts = new ArrayList<>();
        File[] dirs = blogRoot.toFile().listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                if (!dir.getName().equals(ASSETS_FOLDER)) {
                    File indexFile = new File(dir, "index.html");
                    if (indexFile.exists()) {
                        posts.add(dir.getName());
                    }
                }
            }
        }
        posts.sort((a, b) -> b.compareTo(a)); // Newest first
        
        StringBuilder listHtml = new StringBuilder();
        for (String post : posts) {
            String displayName = post.length() > 11 ? post.substring(11).replace("-", " ") : post;
            String date = post.length() >= 10 ? post.substring(0, 10) : "";
            listHtml.append(String.format(
                "<li><a href=\"%s/index.html\">%s</a> <span class=\"date\">%s</span></li>\n",
                post, capitalize(displayName), date
            ));
        }
        
        String indexHtml = """
<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Code Blog Posts</title>
    <link rel="stylesheet" href="assets/styles.css">
</head>
<body>
    <div class="container">
        <header>
            <button class="theme-toggle" onclick="toggleTheme()">🌙</button>
            <h1>📝 Code Documentation</h1>
            <p class="subtitle">AI-generated blog posts about your code</p>
        </header>
        <main>
            <ul class="post-list">
                %s
            </ul>
        </main>
    </div>
    <script src="assets/script.js"></script>
</body>
</html>
""".formatted(listHtml.toString());
        
        try (FileWriter writer = new FileWriter(blogRoot.resolve("index.html").toFile())) {
            writer.write(indexHtml);
        }
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    
    // ========================================================================
    // Static Assets
    // ========================================================================
    
    private String getStylesCss() {
        return """
:root {
    --bg: #ffffff;
    --bg-secondary: #f8f9fa;
    --text: #1a1a2e;
    --text-secondary: #6c757d;
    --accent: #0066ff;
    --code-bg: #1e1e2e;
    --border: #e9ecef;
}

[data-theme="dark"] {
    --bg: #1a1a2e;
    --bg-secondary: #16213e;
    --text: #eaeaea;
    --text-secondary: #a0a0a0;
    --accent: #4dabf7;
    --code-bg: #0f0f1a;
    --border: #2d2d44;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    background: var(--bg);
    color: var(--text);
    line-height: 1.7;
    transition: background 0.3s, color 0.3s;
}

.container {
    max-width: 800px;
    margin: 0 auto;
    padding: 2rem;
}

header {
    text-align: center;
    margin-bottom: 3rem;
    padding-bottom: 2rem;
    border-bottom: 1px solid var(--border);
    position: relative;
}

h1 {
    font-size: 2.8rem;
    font-weight: 900;
    margin-bottom: 0.5rem;
    background: linear-gradient(135deg, var(--accent), #a855f7);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: -0.02em;
}

.subtitle, .date {
    color: var(--text-secondary);
    font-size: 0.95rem;
}

.theme-toggle {
    position: absolute;
    top: 0;
    right: 0;
    background: var(--bg-secondary);
    border: 1px solid var(--border);
    border-radius: 50%;
    width: 40px;
    height: 40px;
    font-size: 1.2rem;
    cursor: pointer;
    transition: transform 0.2s;
}

.theme-toggle:hover { transform: scale(1.1); }

section {
    margin-bottom: 2.5rem;
}

h2 {
    font-size: 1.4rem;
    margin-bottom: 1rem;
    color: var(--text);
}

p { margin-bottom: 1rem; }

ul {
    list-style: none;
    padding-left: 0;
}

ul li {
    padding: 0.5rem 0 0.5rem 1.5rem;
    position: relative;
}

ul li::before {
    content: "→";
    position: absolute;
    left: 0;
    color: var(--accent);
}

.code-container {
    position: relative;
    background: var(--code-bg);
    border-radius: 12px;
    overflow: hidden;
}

.copy-btn {
    position: absolute;
    top: 12px;
    right: 12px;
    background: rgba(255,255,255,0.1);
    border: none;
    color: #fff;
    padding: 6px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 0.85rem;
    transition: background 0.2s;
    z-index: 10;
}

.copy-btn:hover { background: rgba(255,255,255,0.2); }

pre {
    padding: 1.5rem;
    overflow-x: auto;
    margin: 0;
}

code {
    font-family: 'JetBrains Mono', 'Fira Code', monospace;
    font-size: 0.9rem;
}

.post-list li {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem;
    background: var(--bg-secondary);
    border-radius: 8px;
    margin-bottom: 0.75rem;
}

.post-list a {
    color: var(--accent);
    text-decoration: none;
    font-weight: 500;
}

.post-list a:hover { text-decoration: underline; }

footer {
    margin-top: 3rem;
    padding-top: 1.5rem;
    border-top: 1px solid var(--border);
    text-align: center;
    color: var(--text-secondary);
    font-size: 0.85rem;
}

@media (max-width: 600px) {
    .container { padding: 1rem; }
    h1 { font-size: 1.6rem; }
}
""";
    }
    
    private String getScriptJs() {
        return """
function toggleTheme() {
    const html = document.documentElement;
    const current = html.getAttribute('data-theme');
    const next = current === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-theme', next);
    localStorage.setItem('theme', next);
    document.querySelector('.theme-toggle').textContent = next === 'dark' ? '☀️' : '🌙';
}

function copyCode() {
    const code = document.querySelector('pre code');
    if (code) {
        navigator.clipboard.writeText(code.textContent).then(() => {
            const btn = document.querySelector('.copy-btn');
            btn.textContent = '✓ Copied!';
            setTimeout(() => { btn.textContent = '📋 Copy'; }, 2000);
        });
    }
}

// Apply saved theme
(function() {
    const saved = localStorage.getItem('theme');
    if (saved) {
        document.documentElement.setAttribute('data-theme', saved);
        const toggle = document.querySelector('.theme-toggle');
        if (toggle) toggle.textContent = saved === 'dark' ? '☀️' : '🌙';
    }
})();
""";
    }
}
