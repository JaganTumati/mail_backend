package com.disposablemail;

import com.disposablemail.model.Inbox;
import com.disposablemail.repository.InboxRepository;
import com.disposablemail.service.InboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    @Mock
    private InboxRepository inboxRepository;

    @InjectMocks
    private InboxService inboxService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inboxService, "mailDomain",   "tempmail.local");
        ReflectionTestUtils.setField(inboxService, "cleanupHours", 24);
    }

    @Test
    void generateInbox_shouldReturnUniqueAddress() {
        when(inboxRepository.existsByAddress(anyString())).thenReturn(false);
        when(inboxRepository.save(any(Inbox.class))).thenAnswer(i -> i.getArgument(0));

        Inbox inbox = inboxService.generateInbox();

        assertNotNull(inbox);
        assertTrue(inbox.getAddress().endsWith("@tempmail.local"));
        assertTrue(inbox.getIsActive());
        assertNotNull(inbox.getExpiresAt());
        assertTrue(inbox.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void findByAddress_shouldReturnInbox_whenAddressExists() {
        Inbox mockInbox = new Inbox("test@tempmail.local", LocalDateTime.now().plusHours(24));
        when(inboxRepository.findByAddressAndIsActiveTrue("test@tempmail.local"))
                .thenReturn(Optional.of(mockInbox));

        Optional<Inbox> result = inboxService.findByAddress("test@tempmail.local");

        assertTrue(result.isPresent());
        assertEquals("test@tempmail.local", result.get().getAddress());
    }

    @Test
    void findByAddress_shouldReturnEmpty_whenAddressNotFound() {
        when(inboxRepository.findByAddressAndIsActiveTrue(anyString()))
                .thenReturn(Optional.empty());

        Optional<Inbox> result = inboxService.findByAddress("ghost@tempmail.local");

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteInbox_shouldCallRepository() {
        inboxService.deleteInbox(42L);
        verify(inboxRepository, times(1)).deleteById(42L);
    }
}
