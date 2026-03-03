package com.example.taskmanagerapi.modules.boards.domain;

/**
 * Board types - Extensible for future implementations
 * Currently supporting BOARD (default), prepared for KANBAN, CALENDAR, TIMELINE, etc.
 */
public enum BoardType {
    BOARD,  // Default board type with lists and cards
    // Future types can be added here:
    // KANBAN,
    // CALENDAR,
    // TIMELINE,
    // SCRUM,
    // GANTT
}
