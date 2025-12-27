package com.optistockplatrorm.service;

import com.optistockplatrorm.dto.CarrierRequestDTO;
import com.optistockplatrorm.dto.CarrierResponseDTO;
import com.optistockplatrorm.entity.Carrier;
import com.optistockplatrorm.exception.GestionException;
import com.optistockplatrorm.mapper.CarrierMapper;
import com.optistockplatrorm.repository.CarrierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class CarrierServiceTest {

    @Mock private CarrierRepository carrierRepository;
    @Mock private CarrierMapper carrierMapper;

    @InjectMocks
    private CarrierService carrierService;

    private Carrier carrier;
    private CarrierRequestDTO carrierRequestDTO;
    private CarrierResponseDTO carrierResponseDTO;

    @BeforeEach
    void setUp() {
        carrier = Carrier.builder()
                .id(1L)
                .carrierName("Fedex Express")
                .phoneNumber("1234567890")
                .build();

        carrierRequestDTO = new CarrierRequestDTO("Fedex Express", "1234567890");
        carrierResponseDTO = new CarrierResponseDTO(1L, "Fedex Express", "1234567890");
    }

    @Test
    void testGetAllCarriersSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Carrier> carrierPage = new PageImpl<>(List.of(carrier));

        when(carrierRepository.findAll(pageable)).thenReturn(carrierPage);
        when(carrierMapper.toDTO(carrier)).thenReturn(carrierResponseDTO);

        Page<CarrierResponseDTO> result = carrierService.getAllCarriers(0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        verify(carrierRepository).findAll(pageable);
    }

    @Test
    void testGetAllCarriersEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Carrier> carrierPage = new PageImpl<>(Collections.emptyList());

        when(carrierRepository.findAll(pageable)).thenReturn(carrierPage);

        GestionException ex = assertThrows(GestionException.class,
                () -> carrierService.getAllCarriers(0, 10));
        assertEquals("Aucun transporteur trouvé dans le système", ex.getMessage());
    }

    @Test
    void testGetCarrierByIdSuccess() {
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(carrierMapper.toDTO(carrier)).thenReturn(carrierResponseDTO);

        CarrierResponseDTO result = carrierService.getCarrierById(1L);

        assertNotNull(result);
        assertEquals("Fedex Express", result.carrierName());
    }

    @Test
    void testGetCarrierByIdNotFound() {
        when(carrierRepository.findById(anyLong())).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class,
                () -> carrierService.getCarrierById(99L));
        assertEquals("Transporteur non trouvé avec l'ID : 99", ex.getMessage());
    }

    @Test
    void testCreateCarrierSuccess() {
        Carrier newCarrierEntity = Carrier.builder().id(2L).carrierName("New Carrier").phoneNumber("555").build();
        CarrierRequestDTO newCarrierDTO = new CarrierRequestDTO("New Carrier", "555");
        CarrierResponseDTO newCarrierResponse = new CarrierResponseDTO(2L, "New Carrier", "555");

        when(carrierRepository.existsByCarrierName(anyString())).thenReturn(false);
        when(carrierMapper.toEntity(newCarrierDTO)).thenReturn(newCarrierEntity);
        when(carrierRepository.save(newCarrierEntity)).thenReturn(newCarrierEntity);
        when(carrierMapper.toDTO(newCarrierEntity)).thenReturn(newCarrierResponse);

        CarrierResponseDTO result = carrierService.createCarrier(newCarrierDTO);

        assertNotNull(result);
        verify(carrierRepository).save(newCarrierEntity);
    }

    @Test
    void testCreateCarrierNameExists() {
        when(carrierRepository.existsByCarrierName(anyString())).thenReturn(true);

        GestionException ex = assertThrows(GestionException.class,
                () -> carrierService.createCarrier(carrierRequestDTO));
        assertEquals("Un transporteur avec ce nom existe déjà", ex.getMessage());
        verify(carrierRepository, never()).save(any(Carrier.class));
    }

    @Test
    void testUpdateCarrierSuccess() {
        CarrierRequestDTO updatedDTO = new CarrierRequestDTO("DHL Express", "999888777");

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(carrierRepository.existsByCarrierName("DHL Express")).thenReturn(false);
        when(carrierRepository.save(carrier)).thenReturn(carrier);

        CarrierResponseDTO updatedResponse = new CarrierResponseDTO(1L, "DHL Express", "999888777");
        when(carrierMapper.toDTO(carrier)).thenReturn(updatedResponse);

        CarrierResponseDTO result = carrierService.updateCarrier(1L, updatedDTO);

        assertNotNull(result);
        assertEquals("DHL Express", carrier.getCarrierName()); // Verify entity updated
        verify(carrierRepository).save(carrier);
    }

    @Test
    void testUpdateCarrierNotFound() {
        when(carrierRepository.findById(anyLong())).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class,
                () -> carrierService.updateCarrier(99L, carrierRequestDTO));
        assertEquals("Transporteur non trouvé avec l'ID : 99", ex.getMessage());
    }

    @Test
    void testUpdateCarrierNameExistsAndDifferent() {
        CarrierRequestDTO updatedDTO = new CarrierRequestDTO("DHL Express", "999888777");

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(carrierRepository.existsByCarrierName("DHL Express")).thenReturn(true);

        GestionException ex = assertThrows(GestionException.class,
                () -> carrierService.updateCarrier(1L, updatedDTO));

        assertEquals("Un transporteur avec ce nom existe déjà", ex.getMessage());
        verify(carrierRepository, never()).save(any(Carrier.class));
    }

    @Test
    void testUpdateCarrierNameSameButExists() {
        CarrierRequestDTO updatedDTO = new CarrierRequestDTO("Fedex Express", "999888777");

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(carrierRepository.save(carrier)).thenReturn(carrier);

        CarrierResponseDTO updatedResponse = new CarrierResponseDTO(1L, "Fedex Express", "999888777");
        when(carrierMapper.toDTO(carrier)).thenReturn(updatedResponse);

        assertDoesNotThrow(() -> carrierService.updateCarrier(1L, updatedDTO));
        verify(carrierRepository, never()).existsByCarrierName(anyString());
    }

    @Test
    void testDeleteCarrierSuccess() {
        when(carrierRepository.existsById(1L)).thenReturn(true);
        doNothing().when(carrierRepository).deleteById(1L);

        assertDoesNotThrow(() -> carrierService.deleteCarrier(1L));
        verify(carrierRepository).deleteById(1L);
    }

    @Test
    void testDeleteCarrierNotFound() {
        when(carrierRepository.existsById(99L)).thenReturn(false);

        GestionException ex = assertThrows(GestionException.class,
                () -> carrierService.deleteCarrier(99L));
        assertEquals("Transporteur non trouvé avec l'ID : 99", ex.getMessage());
        verify(carrierRepository, never()).deleteById(anyLong());
    }
}