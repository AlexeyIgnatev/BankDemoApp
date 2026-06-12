package com.esom.bank.screens.main.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.esom.bank.databinding.FragmentTransferConfirmationBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransferConfirmationFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentTransferConfirmationBinding
    private var confirmationData: Bundle = Bundle.EMPTY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransferConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            DATA_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            confirmationData = Bundle(bundle)
            binding.title.text = bundle.getString(TITLE_KEY).orEmpty()
        }

        binding.confirmBtn.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_REQUEST_KEY,
                Bundle(confirmationData).apply {
                    putBoolean(CONFIRMED_KEY, true)
                }
            )
            dismiss()
        }
        binding.cancelBtn.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_REQUEST_KEY,
                bundleOf(CONFIRMED_KEY to false)
            )
            dismiss()
        }
    }

    companion object {
        const val DATA_REQUEST_KEY = "transfer_confirmation_data"
        const val RESULT_REQUEST_KEY = "transfer_confirmation_result"
        const val CONFIRMED_KEY = "transfer_confirmation_confirmed"
        const val TITLE_KEY = "transfer_confirmation_title"
        const val OPERATION_KEY = "transfer_confirmation_operation"
        const val AMOUNT_KEY = "transfer_confirmation_amount"
        const val CREDITED_AMOUNT_KEY = "transfer_confirmation_credited_amount"
        const val FROM_CURRENCY_KEY = "transfer_confirmation_from_currency"
        const val TO_CURRENCY_KEY = "transfer_confirmation_to_currency"
        const val PHONE_KEY = "transfer_confirmation_phone"
        const val ADDRESS_KEY = "transfer_confirmation_address"
        const val OPERATION_TITLE_KEY = "transfer_confirmation_operation_title"
        const val PAID_FROM_KEY = "transfer_confirmation_paid_from"
        const val RECIPIENT_KEY = "transfer_confirmation_recipient"
    }
}
