package com.sinedrio.danai.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sinedrio.danai.R
import com.sinedrio.danai.databinding.ActivityChatBinding
import com.sinedrio.danai.senate.chat.PersonaNegotiator

/**
 * The Sinedrio Chat Room.
 *
 * Presents the live multi-agent conversation driven by [ChatViewModel].  The
 * human moderator can type messages or press **Auto** to let the [AutoModerator]
 * generate the next question.  API settings are accessible via the overflow menu.
 *
 * Expected intent extras:
 * - [EXTRA_OWNER_NAME]  (String, optional) — the human moderator's display name.
 * - [EXTRA_OWNER_TOKEN] (String, required) — delegation token.
 * - [EXTRA_TOPIC]       (String, optional) — opening topic for the session.
 */
class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OWNER_NAME = "extra_owner_name"
        const val EXTRA_OWNER_TOKEN = "extra_owner_token"
        const val EXTRA_TOPIC = "extra_topic"
    }

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val adapter = ChatMessageAdapter()
    private lateinit var layoutManager: LinearLayoutManager

    private var ownerName = "Moderatore"
    private var ownerToken = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        ownerName = intent.getStringExtra(EXTRA_OWNER_NAME) ?: "Moderatore"
        ownerToken = intent.getStringExtra(EXTRA_OWNER_TOKEN) ?: "default"
        val topic = intent.getStringExtra(EXTRA_TOPIC) ?: ""

        binding.recyclerChat.adapter = adapter
        binding.recyclerChat.layoutManager = layoutManager

        observeViewModel()
        setupInputActions()

        if (savedInstanceState == null) {
            val sessionTopic = topic.ifBlank { "Che cos'è la Clean Architecture e come si applica ad Android?" }
            viewModel.startSession(ownerName, ownerToken, sessionTopic)
        }
    }

    // ── Menu ───────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_api_settings -> {
                showApiSettingsDialog()
                true
            }
            R.id.action_use_mock -> {
                viewModel.useMockApi()
                Snackbar.make(binding.root, getString(R.string.msg_using_mock), Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_server_settings -> {
                showServerSettingsDialog()
                true
            }
            R.id.action_persona_mode -> {
                showPersonaModeDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ── Input actions ──────────────────────────────────────────────────────────

    private fun setupInputActions() {
        binding.buttonSend.setOnClickListener {
            val text = binding.editMessage.text?.toString()?.trim() ?: ""
            if (text.isNotBlank()) {
                viewModel.sendHumanMessage(ownerName, text)
                binding.editMessage.text?.clear()
            }
        }

        binding.buttonAuto.setOnClickListener {
            viewModel.autoModerate()
        }
    }

    // ── ViewModel observation ──────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages.toList()) {
                if (messages.isNotEmpty()) {
                    binding.recyclerChat.scrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.isProcessing.observe(this) { processing ->
            binding.progressBar.visibility = if (processing) View.VISIBLE else View.GONE
            binding.buttonSend.isEnabled = !processing
            binding.buttonAuto.isEnabled = !processing
        }

        viewModel.session.observe(this) { session ->
            if (session != null) {
                supportActionBar?.subtitle = session.topic.take(50)
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────

    private fun showApiSettingsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }

        val editApiKey = EditText(this).apply { hint = getString(R.string.hint_api_key) }
        val editBaseUrl = EditText(this).apply { hint = getString(R.string.hint_base_url) }
        val editModel = EditText(this).apply { hint = getString(R.string.hint_model) }

        layout.addView(editApiKey)
        layout.addView(editBaseUrl)
        layout.addView(editModel)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_api_settings))
            .setView(layout)
            .setPositiveButton(getString(R.string.action_apply)) { _, _ ->
                viewModel.configureRealApi(
                    apiKey = editApiKey.text.toString().trim(),
                    baseUrl = editBaseUrl.text.toString().trim(),
                    model = editModel.text.toString().trim()
                )
                Snackbar.make(binding.root, getString(R.string.msg_api_configured), Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showServerSettingsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }

        val editServerUrl = EditText(this).apply { hint = getString(R.string.hint_server_url) }
        val editAuthToken = EditText(this).apply { hint = getString(R.string.hint_server_auth) }

        layout.addView(editServerUrl)
        layout.addView(editAuthToken)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_server_settings))
            .setView(layout)
            .setPositiveButton(getString(R.string.action_apply)) { _, _ ->
                val url = editServerUrl.text.toString().trim()
                if (url.isNotBlank()) {
                    viewModel.configureRemoteStorage(url, editAuthToken.text.toString().trim())
                    Snackbar.make(binding.root, getString(R.string.msg_server_configured), Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.useLocalStorage()
                    Snackbar.make(binding.root, getString(R.string.msg_using_local), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showPersonaModeDialog() {
        val modes = arrayOf(
            getString(R.string.persona_mode_default),
            getString(R.string.persona_mode_shuffle),
            getString(R.string.persona_mode_negotiate)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_persona_mode))
            .setItems(modes) { _, which ->
                val mode = when (which) {
                    0 -> PersonaNegotiator.Mode.DEFAULT
                    1 -> PersonaNegotiator.Mode.SHUFFLE
                    2 -> PersonaNegotiator.Mode.NEGOTIATE
                    else -> PersonaNegotiator.Mode.DEFAULT
                }
                viewModel.setNegotiationMode(mode)
                Snackbar.make(binding.root, getString(R.string.msg_persona_mode, modes[which]), Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
