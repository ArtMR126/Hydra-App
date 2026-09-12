package com.example.hydracontrol

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    // Порт 2000 для HydraRoute
    private val defaultUrl = "http://192.168.2.1:2000"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("hydra_app_prefs", Context.MODE_PRIVATE)
        val currentUrl = prefs.getString("custom_url", defaultUrl) ?: defaultUrl

        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectOptimizationCode(view)
                }
            }

            webChromeClient = WebChromeClient()
        }

        // Кнопка быстрой смены адреса роутера в правом верхнем углу
        val settingsBtn = ImageButton(this).apply {
            val size = (36 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = (10 * resources.displayMetrics.density).toInt()
                rightMargin = (10 * resources.displayMetrics.density).toInt()
            }
            setImageResource(android.R.drawable.ic_menu_preferences)
            setBackgroundColor(0x33000000)
            setOnClickListener { showSettingsDialog() }
        }

        rootLayout.addView(webView)
        rootLayout.addView(settingsBtn)
        setContentView(rootLayout)

        webView.loadUrl(currentUrl)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun showSettingsDialog() {
        val prefs = getSharedPreferences("hydra_app_prefs", Context.MODE_PRIVATE)
        val currentUrl = prefs.getString("custom_url", defaultUrl) ?: defaultUrl

        val input = EditText(this).apply {
            setText(currentUrl)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Адрес HydraRoute")
            .setMessage("Укажите IP и порт роутера:")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newUrl = input.text.toString().trim()
                prefs.edit().putString("custom_url", newUrl).apply()
                webView.loadUrl(newUrl)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun injectOptimizationCode(view: WebView?) {
        val customCss = """
            /* Увеличение функциональных кнопок и переключателей */
            .cbi-button, button, input[type='button'], .btn {
                min-height: 42px !important;
                min-width: 44px !important;
                padding: 6px 12px !important;
                font-size: 14px !important;
                margin: 3px !important;
                touch-action: manipulation !important;
            }

            /* Кнопки ручного перемещения вверх/вниз */
            .mobile-order-btn {
                display: inline-block !important;
                padding: 6px 12px !important;
                margin-right: 6px !important;
                background-color: #2a3b50 !important;
                color: #00d2ff !important;
                border: 1px solid #00d2ff !important;
                border-radius: 6px !important;
                font-weight: bold !important;
                font-size: 16px !important;
            }
        """.trimIndent().replace("\n", " ")

        val jsScript = """
            (function() {
                var style = document.getElementById('mobile-hydra-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'mobile-hydra-style';
                    style.innerHTML = "$customCss";
                    document.head.appendChild(style);
                }

                function patchSortRows() {
                    var rows = document.querySelectorAll('.modal-dialog tr, .modal tr, table tr');
                    rows.forEach(function(row) {
                        var firstCell = row.cells ? row.cells[0] : null;
                        if (!firstCell || row.getAttribute('data-patched') === 'true') return;
                        
                        var text = firstCell.innerText || '';
                        var html = firstCell.innerHTML || '';
                        
                        if (text.includes('⋮') || text.includes('::') || html.includes('fa-bars') || firstCell.classList.contains('drag-handle')) {
                            row.setAttribute('data-patched', 'true');
                            
                            var upBtn = document.createElement('span');
                            upBtn.className = 'mobile-order-btn';
                            upBtn.innerText = '▲';
                            upBtn.onclick = function(e) {
                                e.stopPropagation();
                                e.preventDefault();
                                var prev = row.previousElementSibling;
                                if (prev && prev.parentNode) {
                                    prev.parentNode.insertBefore(row, prev);
                                    row.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            };

                            var downBtn = document.createElement('span');
                            downBtn.className = 'mobile-order-btn';
                            downBtn.innerText = '▼';
                            downBtn.onclick = function(e) {
                                e.stopPropagation();
                                e.preventDefault();
                                var next = row.nextElementSibling;
                                if (next && next.parentNode) {
                                    next.parentNode.insertBefore(next, row);
                                    row.dispatchEvent(new Event('change', { bubbles: true }));
                                }
                            };

                            firstCell.insertBefore(downBtn, firstCell.firstChild);
                            firstCell.insertBefore(upBtn, firstCell.firstChild);
                        }
                    });
                }

                patchSortRows();
                new MutationObserver(function() { patchSortRows(); }).observe(document.body, { childList: true, subtree: true });
            })();
        """.trimIndent()

        view?.evaluateJavascript(jsScript, null)
    }
}
