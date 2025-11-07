package com.example.radio.view

import com.example.radio.model.Program

interface ProgramsView {
    fun showPrograms(programs: List<Program>)
    fun showProgramsError(message: String)
}