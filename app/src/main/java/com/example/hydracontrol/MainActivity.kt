package com.example.hydracontrol

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val defaultUrl = "http://192.168.2.1"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("hydra_app_prefs", Context.MODE_PRIVATE)
        val targetHost = prefs.getString("custom_url", defaultUrl) ?: defaultUrl

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

        setContentView(webView)
        webView.loadUrl(targetHost)

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

    private fun injectOptimizationCode(view: WebView?) {
        val customCss = """
            /* Увеличение кнопок управления (bal, mux, via, noise, frag, rfp) */
            .cbi-button, button, input[type='button'], .btn {
                min-height: 40px !important;
                min-width: 44px !important;
                padding: 6px 12px !important;
                font-size: 14px !important;
                margin: 3px !important;
                touch-action: manipulation !important;
            }

            /* Увеличение бейджей и статусов */
            .badge, span[class*='badge'] {
                font-size: 13px !important;
                padding: 5px 8px !important;
            }

            /* Кнопки ручного перемещения вверх/вниз */
            .mobile-order-btn {
                display: inline-block !important;
                padding: 4px 10px !important;
                margin-right: 4px !important;
                background-color: #2a3b50 !important;
                color: #00d2ff !important;
                border: 1px solid #00d2ff !important;
                border-radius: 4px !important;
                font-weight: bold !important;
                font-size: 14px !important;
            }
        """.trimIndent().replace("\n", " ")

        val jsScript = """
            (function() {
                // 1. Инъекция стилей
                var style = document.getElementById('mobile-hydra-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'mobile-hydra-style';
                    style.innerHTML = "$customCss";
                    document.head.appendChild(style);
                }

                // 2. Внедрение кнопок перемещения ▲ и ▼ перед элементами сортировки
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
