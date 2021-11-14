package de.js.app.agtracker.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import de.js.app.agtracker.MainActivityNav
import de.js.app.agtracker.databinding.FragmentExportFramentBinding
import de.js.app.agtracker.util.KMLUtil
import de.js.app.agtracker.util.Util

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ExportFrament.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExportFrament : Fragment() {
    private lateinit var createDocument: ActivityResultLauncher<String>

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentExportFramentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            saveShp(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExportFramentBinding.inflate(inflater, container, false)

        binding.btnExportKml.setOnClickListener { onExportShpClick(it) }

        return binding.root
    }

    private fun onExportShpClick(view: View?) {
        createDocument.launch("AGTracker_${Util.getTimestampPath()}.kml")
    }

    private fun saveShp(uri: Uri?) {
        Log.d(this.javaClass.simpleName, uri.toString())
        Log.d(this.javaClass.simpleName, uri?.path ?: "")
        val mainActivity = activity as MainActivityNav

        val kmlString = KMLUtil().createKML(mainActivity.dbHandler!!)

        Log.d(this.javaClass.simpleName, kmlString)

        if (uri != null) {
            val outputStream = requireContext().contentResolver.openOutputStream(uri)
            outputStream?.write(kmlString.toByteArray())
            outputStream?.close()
        }


    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ExportFrament.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ExportFrament().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }


    }
}