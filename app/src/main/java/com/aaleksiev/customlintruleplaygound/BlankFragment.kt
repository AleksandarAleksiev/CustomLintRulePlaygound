package com.aaleksiev.customlintruleplaygound

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.aaleksiev.customlintruleplaygound.databinding.FragmentBlankBinding

class BlankFragment : Fragment(R.layout.fragment_blank) {

    val binding by lazy { FragmentBlankBinding.bind(requireView()) }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.imageView.alpha = .5f
    }

    companion object {
        fun newInstance() = BlankFragment()
    }
}