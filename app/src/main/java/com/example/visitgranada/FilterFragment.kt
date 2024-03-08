package com.example.visitgranada

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * Fragmento usado para mostrar el filtro de los lugares.
 * @property categoriasLinearLayout Linear Layout donde se encuentran los lugares.
 * @property listener Listener de [DialogFragment]
 * @property categorias Lista de categorías seleccionadas.
 */
class FilterFragment : DialogFragment(){

    private lateinit var categoriasLinearLayout: LinearLayout
    private lateinit var listener: DialogFragmentListener
    private var categorias: MutableList<String> = arrayListOf()

    /**
     * Método llamado cuando el fragmento se adjunta a una actividad.
     * Verifica que la actividad implemente la interfaz DialogFragmentListener.
     */

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as DialogFragmentListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement DialogFragmentListener")
        }
    }

    /**
     * Función que se ejecuta cuando se crea el Dialog.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Filtrar Lugares")
        val rootView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_filter, null)

        // Botones
        val cbArquitectura = rootView.findViewById<CheckBox>(R.id.checkbox_arch)
        val cbCultural = rootView.findViewById<CheckBox>(R.id.checkbox_cultural)
        val cbHistorico = rootView.findViewById<CheckBox>(R.id.checkbox_historic)
        val cbIndustrial = rootView.findViewById<CheckBox>(R.id.checkbox_industrial)
        val cbNatural = rootView.findViewById<CheckBox>(R.id.checkbox_natural)
        val cbReligion = rootView.findViewById<CheckBox>(R.id.checkbox_religion)
        val cbOtros = rootView.findViewById<CheckBox>(R.id.checkbox_otros)

        // Linear Layout
        categoriasLinearLayout = rootView.findViewById(R.id.categorias_linear_layout)
        // Botón Aceptar
        val applyButton = rootView.findViewById<Button>(R.id.btn_apply)
        applyButton.setOnClickListener {
            val categorias = getSelectedCategorias()
            Log.d("Categorías Fragment: ",categorias.toString())
            listener.onPositiveButtonClicked(categorias)
            dismiss()
        }

        // Botón Cancelar
        val cancelButton = rootView.findViewById<Button>(R.id.btn_cancel)
        cancelButton.setOnClickListener {
            listener.onPositiveButtonClicked(listOf())
            dismiss()
        }

        // Configuramos los valores del filtro
        categorias = arguments?.getStringArrayList("cate")!!
        cbArquitectura.isChecked = isCategoriaSeleccionada("architecture")
        cbCultural.isChecked = isCategoriaSeleccionada("cultural")
        cbHistorico.isChecked = isCategoriaSeleccionada("historic")
        cbIndustrial.isChecked = isCategoriaSeleccionada("industrial_facilities")
        cbNatural.isChecked = isCategoriaSeleccionada("natural")
        cbReligion.isChecked = isCategoriaSeleccionada("religion")
        cbOtros.isChecked = isCategoriaSeleccionada("other")

        builder.setView(rootView)
        return builder.create()
    }

    /**
     * Función usada para comprbar las categorías seleccionadas del filtro.
     * @return La lista de categorías seleccionadas.
     */
    private fun getSelectedCategorias(): List<String> {
        val selectedCategorias: MutableList<String> = mutableListOf()

        for (i in 0 until categoriasLinearLayout.childCount) {
            val childView = categoriasLinearLayout.getChildAt(i)
            if (childView is CheckBox && childView.isChecked) {
                when (i) {
                    0 -> {
                        val categoria = "architecture"
                        selectedCategorias.add(categoria)
                    }
                    1 -> {
                        val categoria = "cultural"
                        selectedCategorias.add(categoria)
                    }
                    2 -> {
                        val categoria = "historic"
                        selectedCategorias.add(categoria)
                    }
                    3 -> {
                        val categoria = "industrial_facilities"
                        selectedCategorias.add(categoria)
                    }
                    4 -> {
                        val categoria = "natural"
                        selectedCategorias.add(categoria)
                    }
                    5 -> {
                        val categoria = "religion"
                        selectedCategorias.add(categoria)
                    }
                    else -> {
                        val categoria = "other"
                        selectedCategorias.add(categoria)
                    }
                }
            }
        }

        return selectedCategorias
    }

    /**
     * Función para comprobar si una categoría está seleccionada.
     * @param cat Categoría a comprobar.
     * @return True si está seleccionada, False en caso contrario.
     */
    private fun isCategoriaSeleccionada(cat: String):Boolean{
        return categorias.contains(cat)
    }

}