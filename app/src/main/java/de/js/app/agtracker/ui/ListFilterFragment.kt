package de.js.app.agtracker.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.EndIconMode
import de.js.app.agtracker.R
import de.js.app.agtracker.databinding.FragmentListFilterBinding
import de.js.app.agtracker.viewmodels.TrackedPlacesListViewModel
import java.util.*

private const val LOG_TAG = "ListFilterFragment"


class ListFilterFragment() : BottomSheetDialogFragment(){

    private lateinit var binding: FragmentListFilterBinding
    private lateinit var listFilterViewModel: ListFilterViewModel
    private val placesListViewModel: TrackedPlacesListViewModel by activityViewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        listFilterViewModel = ViewModelProvider(activity).get(ListFilterViewModel::class.java)

        binding.txtDateFrom.setText(listFilterViewModel.dateFrom.value)
        binding.txtDateTo.setText(listFilterViewModel.dateTo.value)
        binding.txtName.setText(listFilterViewModel.name.value)

        binding.btnSaveFilter.setOnClickListener {
            listFilterViewModel.dateFrom.value = binding.txtDateFrom.text.toString()
            listFilterViewModel.dateTo.value = binding.txtDateTo.text.toString()
            listFilterViewModel.name.value = binding.txtName.text.toString()
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListFilterBinding.inflate(inflater, container, false)

        //Set end icons to date inputs for date picker
        with(binding.txtDateFromLayout) {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(R.drawable.ic_baseline_calendar_24)
            setEndIconOnClickListener { showDatePickerDialog(binding.txtDateFrom) }
        }
        with(binding.txtDateToLayout) {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(R.drawable.ic_baseline_calendar_24)
            setEndIconOnClickListener { showDatePickerDialog(binding.txtDateFrom) }
        }

        return binding.root

    }

    private fun showDatePickerDialog(v: View) {
        val datePicker = DatePickerFragment(v as TextInputEditText)
        datePicker.show(parentFragmentManager, "datePicker")
        Log.d(LOG_TAG, "view model size: ${placesListViewModel.trackedPlaces.value?.size.toString()}")
    }

    /**
     * Date Picker
     */
    class DatePickerFragment(private val txtInput: TextInputEditText) : DialogFragment(),
        DatePickerDialog.OnDateSetListener {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            return DatePickerDialog(requireContext(), this, year, month, day)
        }
        override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
            val monthString = if (month < 10) "0${month + 1}" else "${month + 1}"
            val dayString = if (dayOfMonth < 10) "0${dayOfMonth}" else "$dayOfMonth"
            val selectedDate = "$year-$monthString-$dayString"
            txtInput.setText(selectedDate)
            Log.d(LOG_TAG, "onDateSet: $selectedDate")
        }

    }
}


class ListFilterViewModel : ViewModel() {

    var dateFrom: MutableLiveData<String> = MutableLiveData<String>()
    var dateTo: MutableLiveData<String> = MutableLiveData<String>()
    var name: MutableLiveData<String> = MutableLiveData<String>()

}