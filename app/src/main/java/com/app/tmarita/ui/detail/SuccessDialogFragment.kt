package com.app.tmarita.ui.detail

import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.app.tmarita.R
import com.app.tmarita.databinding.DialogSuccessBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SuccessDialogFragment : DialogFragment() {

    private var _binding: DialogSuccessBinding? = null
    private val binding get() = _binding!!

    private var message: String = "¡Listo!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        isCancelable = true
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.4f)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvMessage.text = message

        binding.ivCheck.scaleX = 0f
        binding.ivCheck.scaleY = 0f
        val scaleX = ObjectAnimator.ofFloat(binding.ivCheck, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivCheck, "scaleY", 0f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 380
            interpolator = OvershootInterpolator()
            start()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)
            if (isAdded) dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun show(fragmentManager: androidx.fragment.app.FragmentManager, message: String) {
            SuccessDialogFragment().apply {
                this.message = message
            }.show(fragmentManager, "success_dialog")
        }
    }
}