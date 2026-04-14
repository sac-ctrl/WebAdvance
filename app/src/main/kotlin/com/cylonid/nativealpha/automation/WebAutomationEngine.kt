package com.cylonid.nativealpha.automation

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Web automation engine for auto-clicking, auto-scrolling, and custom scripts
 */
class WebAutomationEngine(
    private val appId: Long
) {
    private val _scripts = MutableStateFlow<List<AutomationScript>>(emptyList())
    val scripts: StateFlow<List<AutomationScript>> = _scripts.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _executionLog = MutableStateFlow<List<String>>(emptyList())
    val executionLog: StateFlow<List<String>> = _executionLog.asStateFlow()

    /**
     * Create new automation script
     */
    fun createScript(
        name: String,
        description: String,
        code: String,
        autoRun: Boolean = false
    ): AutomationScript {
        val script = AutomationScript(
            id = System.currentTimeMillis(),
            appId = appId,
            name = name,
            description = description,
            code = code,
            autoRun = autoRun,
            createdAt = System.currentTimeMillis(),
            lastExecuted = null,
            executionCount = 0,
            isActive = true
        )
        
        val scripts = _scripts.value.toMutableList()
        scripts.add(script)
        _scripts.value = scripts
        
        return script
    }

    /**
     * Update automation script
     */
    fun updateScript(script: AutomationScript) {
        val scripts = _scripts.value.toMutableList()
        val index = scripts.indexOfFirst { it.id == script.id }
        if (index >= 0) {
            scripts[index] = script
            _scripts.value = scripts
        }
    }

    /**
     * Delete automation script
     */
    fun deleteScript(scriptId: Long) {
        val scripts = _scripts.value.toMutableList()
        scripts.removeAll { it.id == scriptId }
        _scripts.value = scripts
    }

    /**
     * Execute single script
     */
    fun executeScript(webView: WebView, script: AutomationScript) {
        _isRunning.value = true
        addLog("Executing script: ${script.name}")
        
        webView.evaluateJavascript(script.code) { result ->
            addLog("Script completed: ${script.name}")
            addLog("Result: $result")
            
            val scripts = _scripts.value.toMutableList()
            val index = scripts.indexOfFirst { it.id == script.id }
            if (index >= 0) {
                scripts[index] = scripts[index].copy(
                    lastExecuted = System.currentTimeMillis(),
                    executionCount = scripts[index].executionCount + 1
                )
                _scripts.value = scripts
            }
            
            _isRunning.value = false
        }
    }

    /**
     * Auto-scroll page
     */
    fun autoScroll(
        webView: WebView,
        direction: ScrollDirection = ScrollDirection.DOWN,
        speed: Int = 3,
        iterations: Int = 10
    ) {
        val scrollAmount = speed * 100
        var currentIteration = 0
        
        val script = when (direction) {
            ScrollDirection.DOWN -> """
                (function() {
                    window.scrollBy(0, $scrollAmount);
                })();
            """.trimIndent()
            
            ScrollDirection.UP -> """
                (function() {
                    window.scrollBy(0, -$scrollAmount);
                })();
            """.trimIndent()
        }
        
        addLog("Auto-scroll started: $direction")
        
        val scrollTimer = object : Thread() {
            override fun run() {
                while (currentIteration < iterations && _isRunning.value) {
                    webView.evaluateJavascript(script) { }
                    currentIteration++
                    Thread.sleep((100 / speed).toLong())
                }
                addLog("Auto-scroll completed")
            }
        }
        
        _isRunning.value = true
        scrollTimer.start()
    }

    /**
     * Auto-click element
     */
    fun autoClick(
        webView: WebView,
        selector: String,
        clickCount: Int = 1,
        delayMs: Long = 500
    ) {
        var clicks = 0
        
        val script = """
            (function() {
                const element = document.querySelector('$selector');
                if (element) {
                    element.click();
                    return 'Clicked: ' + element.tagName;
                }
                return 'Element not found: $selector';
            })();
        """.trimIndent()
        
        addLog("Auto-click started: $selector")
        
        val clickTimer = object : Thread() {
            override fun run() {
                while (clicks < clickCount && _isRunning.value) {
                    webView.evaluateJavascript(script) { result ->
                        addLog("Click result: $result")
                    }
                    clicks++
                    if (clicks < clickCount) Thread.sleep(delayMs)
                }
                addLog("Auto-click completed: $clickCount clicks")
            }
        }
        
        _isRunning.value = true
        clickTimer.start()
    }

    /**
     * Fill form fields
     */
    fun fillFormFields(
        webView: WebView,
        fields: Map<String, String>
    ) {
        addLog("Filling form fields: ${fields.size}")
        
        fields.forEach { (selector, value) ->
            val script = """
                (function() {
                    const field = document.querySelector('$selector');
                    if (field) {
                        field.value = '$value';
                        field.dispatchEvent(new Event('input', { bubbles: true }));
                        field.dispatchEvent(new Event('change', { bubbles: true }));
                        return 'Filled: ' + field.tagName;
                    }
                    return 'Field not found: $selector';
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(script) { result ->
                addLog("Fill result: $result")
            }
        }
    }

    /**
     * Submit form
     */
    fun submitForm(webView: WebView, selector: String = "form") {
        val script = """
            (function() {
                const form = document.querySelector('$selector');
                if (form) {
                    form.submit();
                    return 'Form submitted';
                }
                return 'Form not found: $selector';
            })();
        """.trimIndent()
        
        addLog("Submitting form: $selector")
        
        webView.evaluateJavascript(script) { result ->
            addLog("Submit result: $result")
        }
    }

    /**
     * Load more (infinite scroll support)
     */
    fun autoLoadMore(
        webView: WebView,
        selector: String = "button.load-more, a.load-more, .infinite-scroll",
        iterations: Int = 5,
        delayMs: Long = 2000
    ) {
        addLog("Auto-load-more started: $selector")
        
        var attempts = 0
        
        val loadMoreTimer = object : Thread() {
            override fun run() {
                while (attempts < iterations && _isRunning.value) {
                    val script = """
                        (function() {
                            const element = document.querySelector('$selector');
                            if (element) {
                                element.click();
                                window.scrollBy(0, window.innerHeight);
                                return 'Load-more clicked';
                            }
                            return 'Load-more button not found';
                        })();
                    """.trimIndent()
                    
                    webView.evaluateJavascript(script) { result ->
                        addLog("Load-more result: $result")
                    }
                    
                    attempts++
                    Thread.sleep(delayMs)
                }
                addLog("Auto-load-more completed")
            }
        }
        
        _isRunning.value = true
        loadMoreTimer.start()
    }

    /**
     * Run all auto-run scripts
     */
    fun runAutoRunScripts(webView: WebView) {
        val autoRunScripts = _scripts.value.filter { it.autoRun && it.isActive }
        addLog("Running ${autoRunScripts.size} auto-run scripts")
        autoRunScripts.forEach { executeScript(webView, it) }
    }

    /**
     * Stop all running operations
     */
    fun stopAll() {
        _isRunning.value = false
        addLog("All operations stopped")
    }

    /**
     * Clear execution log
     */
    fun clearLog() {
        _executionLog.value = emptyList()
    }

    private fun addLog(message: String) {
        val log = _executionLog.value.toMutableList()
        log.add(0, "[${System.currentTimeMillis()}] $message")
        _executionLog.value = log.take(1000) // Keep last 1000 logs
    }

    enum class ScrollDirection {
        UP, DOWN
    }

    data class AutomationScript(
        val id: Long,
        val appId: Long,
        val name: String,
        val description: String,
        val code: String,
        val autoRun: Boolean,
        val createdAt: Long,
        val lastExecuted: Long?,
        val executionCount: Int,
        val isActive: Boolean
    )
}
