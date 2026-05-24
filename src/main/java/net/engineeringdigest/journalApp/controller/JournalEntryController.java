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

    /**
     * GET /journal/id/{id}
     * Retrieves a single journal entry by its ID.
     *
     * @param id - The unique identifier of the journal entry to retrieve (from URL path variable)
     * @return the JournalEntry object if found,
     *         or an empty JournalEntry if not found or an error occurs
     *
     * ⚠️ Returning an empty JournalEntry is misleading to the client.
     *    In production, throw a proper exception or return ResponseEntity with
     *    HTTP 404 (Not Found) if the entry doesn't exist, or 500 on server error.
     *
     * @apiNote This method uses HashMap.get() which returns null if key doesn't exist.
     *          Consider handling null gracefully or returning an Optional<JournalEntry>.
     */
    @GetMapping("/id/{id}")                             // Maps to GET /journal/id/{id}
    public JournalEntry findById(@PathVariable Long id) { // Extract 'id' from URL path
        try {
            return journalEntryMap.get(id);             // Retrieve entry by ID from map (nullable)
        } catch (Exception e) {
            // ⚠️ Swallowing the exception with printStackTrace is a bad practice.
            //    Prefer: throw new RuntimeException("Failed to retrieve entry with ID: " + id, e);
            //    Or use @ExceptionHandler / @ControllerAdvice for global error handling.
            e.printStackTrace();
            return new JournalEntry();                  // Returns an empty object — not ideal
        }
    }

    /**
     * DELETE /journal/id/{id}
     * Deletes the journal entry with the given ID.
     *
     * @param id - The unique identifier of the journal entry to delete (from URL path variable)
     * @return true if the entry existed and was removed, false if the entry was not found or an error occurred
     *
     * Notes:
     *  - Returning a Boolean is simple but not expressive; prefer ResponseEntity<Void> with
     *    HTTP 204 (No Content) on success and 404 (Not Found) when the ID does not exist.
     *  - This method checks containsKey() then remove() to indicate whether removal occurred.
     */
    @DeleteMapping("/id/{id}")
    public Boolean deleteById(@PathVariable Long id) {
        try {
            if (!journalEntryMap.containsKey(id)) { // If the key doesn't exist, nothing to delete
                return false;
            }
            journalEntryMap.remove(id); // Remove the entry from the in-memory map
            return true; // Indicate successful deletion
        } catch (Exception e) {
            // Prefer proper exception handling; logging/metrics would be helpful here
            e.printStackTrace();
            return false;
        }
    }

    /**
     * PUT /journal/id/{id}
     * Updates an existing journal entry identified by the given ID.
     *
     * @param id    - The unique identifier of the entry to update (from URL path)
     * @param entry - The updated journal entry data from the request body
     * @return the updated JournalEntry on success,
     *         or an empty JournalEntry if not found or an error occurs
     *
     * ⚠️ BUG IN ORIGINAL: journalEntryMap.put(entry.getId(), entry) uses the ID
     *    from the request BODY, not from the URL path variable.
     *    If the client sends a different ID in the body, it creates a new entry
     *    instead of updating the existing one. Always use the path {id} for updates.
     *
     * ⚠️ Returning an empty JournalEntry on not-found or error is misleading.
     *    In production, return ResponseEntity<JournalEntry> with:
     *    - HTTP 200 (OK)       on success
     *    - HTTP 404 (Not Found) if ID doesn't exist
     *    - HTTP 400 (Bad Request) if body is invalid
     */
    @PutMapping("/id/{id}")
    public JournalEntry updateJournal(@PathVariable Long id, @RequestBody JournalEntry entry) {
        try {
            // Guard clause: return early if the entry doesn't exist
            if (!journalEntryMap.containsKey(id)) {
                // ⚠️ Prefer: throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found: " + id);
                return new JournalEntry();
            }

            // ✅ FIX: Always use the path variable `id`, not entry.getId()
            //    This ensures the correct entry is updated regardless of what
            //    ID the client sends inside the request body.
            entry.setId(id);
            journalEntryMap.put(id, entry); // Replace the existing entry with updated data

            return entry; // Return the updated entry

        } catch (Exception e) {
            // ⚠️ Swallowing exceptions is bad practice.
            //    Prefer: throw new RuntimeException("Failed to update entry with ID: " + id, e);
            //    Or use @ControllerAdvice for centralized global exception handling.
            e.printStackTrace();
            return new JournalEntry(); // Returns empty object — not meaningful to the client
        }
    }

    // ─────────────────────────────────────────────
    // IMPLEMENTATION NOTES (placed at end of class)
    // ─────────────────────────────────────────────
    // - Thread-safety: This controller uses a plain HashMap which is NOT thread-safe.
    //   If the app will handle concurrent requests (typical for web apps), replace
    //   with a ConcurrentHashMap or add synchronization around accesses.
    //
    // - Error handling: Methods currently return primitive success indicators or
    //   empty objects on error. For clear API semantics, return ResponseEntity<T>
    //   and use proper HTTP status codes (e.g., 201 Created, 400 Bad Request,
    //   404 Not Found, 500 Internal Server Error). Use @ControllerAdvice for
    //   centralized exception handling.
    //
    // - ID generation: The randomIdGenerator() creates time-ordered IDs suitable for
    //   in-memory/demo use. For production, prefer DB-generated IDs, UUIDs, or a
    //   robust distributed ID generator (e.g., Snowflake) if you need uniqueness
    //   across multiple instances.
    //
    // - Persistence: This implementation is in-memory and data will be lost on
    //   application restart. Integrate JPA/Hibernate or another persistence
    //   mechanism when moving beyond prototypes.
    //
    // - Tests: Add unit tests for the controller and integration tests for the
    //   API endpoints (use MockMvc or WebTestClient). Also test concurrent access
    //   scenarios if you switch to a shared in-memory store.
    //
    // - Validation: Validate request bodies (e.g., @Valid + javax.validation)
    //   so malformed or incomplete journal entries are rejected with 400.

}