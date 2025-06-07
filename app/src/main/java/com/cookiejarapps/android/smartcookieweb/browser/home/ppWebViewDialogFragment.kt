package com.cookiejarapps.android.smartcookieweb.browser.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.cookiejarapps.android.smartcookieweb.R

class InAppWebViewDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_URL = "url"

        fun newInstance(url: String): InAppWebViewDialogFragment {
            val frag = InAppWebViewDialogFragment()
            val args = Bundle()
            args.putString(ARG_URL, url)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_in_app_webview, container, false)

        val webView = view.findViewById<WebView>(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        val url = arguments?.getString(ARG_URL) ?: "about:blank"
        webView.loadUrl(url)

        // Navigation och stäng
        val btnBack = view.findViewById<ImageButton>(R.id.btn_back)
        val btnForward = view.findViewById<ImageButton>(R.id.btn_forward)
        val btnClose = view.findViewById<TextView>(R.id.btn_close)

        btnBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        btnClose.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
