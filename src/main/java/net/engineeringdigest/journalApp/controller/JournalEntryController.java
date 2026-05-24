package net.engineeringdigest.journalApp.controller;

import net.engineeringdigest.journalApp.entity.JournalEntry;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * REST Controller for managing journal entries.
 *
 * Base URL: /journal
 *
 * This is an in-memory implementation — data is lost on restart.
 * No database or repository layer is used here.
 */
@RestController                 // Marks this class as a REST controller (combines @Controller + @ResponseBody)
@RequestMapping("journal")      // All endpoints in this class are prefixed with /journal
public class JournalEntryController {

    /**
     * In-memory store for journal entries.
     * Key   → unique entry ID (Long)
     * Value → JournalEntry object
     *
     * NOTE: Not thread-safe. Use ConcurrentHashMap if handling concurrent requests.
     */
    private final Map<Long, JournalEntry> journalEntryMap = new HashMap<>();

    // ─────────────────────────────────────────────
    // UTILITY
    // ─────────────────────────────────────────────

    /**
     * Generates a pseudo-unique Long ID by combining:
     *   - Current epoch time in milliseconds  → ensures time-ordering
     *   - A random suffix (0 – 999,999)       → reduces same-millisecond collisions
     *
     * Formula: epochMillis * 1_000_000 + randomSuffix
     *
     * ⚠️ Not guaranteed to be globally unique (no UUID, no DB sequence).
     *    Fine for in-memory/demo use; replace with UUID or DB-generated ID in production.
     */
    private Long randomIdGenerator() {
        long epochMillis = Instant.now().toEpochMilli();
        int suffix = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return epochMillis * 1_000_000L + suffix;
    }

    // ─────────────────────────────────────────────
    // ENDPOINTS
    // ─────────────────────────────────────────────

    /**
     * GET /journal
     * Returns all journal entries as a list.
     *
     * journalEntryMap.values() returns a Collection, so we wrap it
     * in a new ArrayList to return a proper List type.
     */
    @GetMapping
    public List<JournalEntry> getJournalEntries() {
        return new ArrayList<>(journalEntryMap.values());
    }

    /**
     * POST /journal
     * Accepts a JSON body, creates a new journal entry, stores and returns it.
     *
     * @param journalEntry - Deserialized from the HTTP request body via @RequestBody
     * @return the saved JournalEntry with its generated ID,
     *         or an empty JournalEntry if something goes wrong
     *
     * ⚠️ Returning an empty JournalEntry on error is misleading to the client.
     *    In production, throw a proper exception or return ResponseEntity with
     *    an appropriate HTTP status code (e.g., 400 or 500).
     */
    @PostMapping
    public JournalEntry createEntry(@RequestBody JournalEntry journalEntry) {
        try {
            journalEntry.setId(randomIdGenerator());            // Assign a generated ID
            journalEntryMap.put(journalEntry.getId(), journalEntry); // Store in map
            return journalEntry;                                // Return the saved entry
        } catch (Exception e) {
            // ⚠️ Swallowing the exception with printStackTrace is a bad practice.
            //    Prefer: throw new RuntimeException("Failed to save entry", e);
            //    Or use @ExceptionHandler / @ControllerAdvice for global error handling.
            e.printStackTrace();
            return new JournalEntry(); // Returns an empty object — not ideal
        }
    }
}