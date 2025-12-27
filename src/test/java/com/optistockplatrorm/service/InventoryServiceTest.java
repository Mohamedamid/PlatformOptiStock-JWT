package com.optistockplatrorm.service;

import com.optistockplatrorm.dto.*;
import com.optistockplatrorm.entity.*;
import com.optistockplatrorm.entity.Enums.MovementType;
import com.optistockplatrorm.entity.Enums.OrderLineStatus;
import com.optistockplatrorm.exception.GestionException;
import com.optistockplatrorm.mapper.InventoryMapper;
import com.optistockplatrorm.mapper.MovementInventoryMapper;
import com.optistockplatrorm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryMapper inventoryMapper;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private WareHouseRepository entrepotRepository;
    @Mock
    private MovementInventoryRepository mouvementRepository;
    @Mock
    private MovementInventoryMapper mouvementMapper;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;
    private InventoryRequestDTO inventoryRequestDTO;
    private InventoryResponseDTO inventoryResponseDTO;
    private MovementInventoryResponseDTO movementResponseDTO;

    @BeforeEach
    void setup() {
        product = new Product();
        product.setId(1L);
        product.setName("Laptop");

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Main Warehouse");

        inventory = new Inventory();
        inventory.setId(100L);
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setQuantityOnHand(50);
        inventory.setQuantityReserved(10);

        inventoryRequestDTO = new InventoryRequestDTO(1L, 10L);
        inventoryResponseDTO = new InventoryResponseDTO(100L, 50, 10, "Laptop", "Main Warehouse");

        movementResponseDTO = new MovementInventoryResponseDTO(
                200L, 5, 100L, MovementType.INBOUND, LocalDateTime.now());
    }

    @Test
    void testCreateInventorySuccess() {
        Inventory newInventory = new Inventory();
        newInventory.setProduct(product);
        newInventory.setWarehouse(warehouse);

        when(inventoryMapper.toEntity(inventoryRequestDTO)).thenReturn(newInventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(entrepotRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryMapper.toDto(inventory)).thenReturn(inventoryResponseDTO);

        InventoryResponseDTO result = inventoryService.createInventory(inventoryRequestDTO);

        assertNotNull(result);
        assertEquals(0, newInventory.getQuantityOnHand());
        assertEquals(0, newInventory.getQuantityReserved());
        verify(inventoryRepository).save(newInventory);
    }

    @Test
    void testCreateInventoryProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        InventoryRequestDTO dto = new InventoryRequestDTO(99L, 10L);

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.createInventory(dto));
        assertEquals("Produit introuvable avec l’identifiant 99", ex.getMessage());
    }

    @Test
    void testCreateInventoryWarehouseNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(entrepotRepository.findById(99L)).thenReturn(Optional.empty());
        InventoryRequestDTO dto = new InventoryRequestDTO(1L, 99L);

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.createInventory(dto));
        assertEquals("Entrepôt introuvable avec l’identifiant 99", ex.getMessage());
    }

    @Test
    void testCreateInventoryAlreadyExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(entrepotRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(inventory));

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.createInventory(inventoryRequestDTO));
        assertEquals("Un inventaire existe déjà pour ce produit dans cet entrepôt.", ex.getMessage());
    }

    @Test
    void testGetInventoryByIdSuccess() {
        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(inventoryMapper.toDto(inventory)).thenReturn(inventoryResponseDTO);

        InventoryResponseDTO result = inventoryService.getInventoryById(100L);

        assertNotNull(result);
        assertEquals(100L, result.id());
    }

    @Test
    void testGetInventoryByIdNotFound() {
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.getInventoryById(999L));
        assertEquals("Inventaire introuvable avec l’identifiant 999", ex.getMessage());
    }

    @Test
    void testGetAllInventory() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> inventoryPage = new PageImpl<>(List.of(inventory));

        when(inventoryRepository.findAll(pageable)).thenReturn(inventoryPage);
        when(inventoryMapper.toDto(inventory)).thenReturn(inventoryResponseDTO);

        Page<InventoryResponseDTO> result = inventoryService.getAllInventory(0, 10);

        assertEquals(1, result.getContent().size());
        verify(inventoryRepository).findAll(pageable);
    }

    @Test
    void testUpdateInventorySuccessFullUpdate() {
        Product newProduct = new Product();
        newProduct.setId(2L);
        newProduct.setName("Monitor");
        Warehouse newWarehouse = new Warehouse();
        newWarehouse.setId(20L);
        newWarehouse.setName("Secondary Warehouse");

        InventoryRequestDTO updateDTO = new InventoryRequestDTO(2L, 20L);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(productRepository.findById(2L)).thenReturn(Optional.of(newProduct));
        when(entrepotRepository.findById(20L)).thenReturn(Optional.of(newWarehouse));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        inventoryService.updateInventory(100L, updateDTO);

        assertEquals(newProduct, inventory.getProduct());
        assertEquals(newWarehouse, inventory.getWarehouse());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void testUpdateInventoryOnlyProduct() {
        Product newProduct = new Product();
        newProduct.setId(2L);
        newProduct.setName("Monitor");

        InventoryRequestDTO updateDTO = new InventoryRequestDTO(2L, null);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(productRepository.findById(2L)).thenReturn(Optional.of(newProduct));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        inventoryService.updateInventory(100L, updateDTO);

        assertEquals(newProduct, inventory.getProduct());
        assertEquals(warehouse, inventory.getWarehouse());
        verify(entrepotRepository, never()).findById(anyLong());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void testUpdateInventoryOnlyWarehouse() {
        Warehouse newWarehouse = new Warehouse();
        newWarehouse.setId(20L);
        newWarehouse.setName("Secondary Warehouse");

        InventoryRequestDTO updateDTO = new InventoryRequestDTO(null, 20L);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(entrepotRepository.findById(20L)).thenReturn(Optional.of(newWarehouse));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        inventoryService.updateInventory(100L, updateDTO);

        assertEquals(newWarehouse, inventory.getWarehouse());
        assertEquals(product, inventory.getProduct());
        verify(productRepository, never()).findById(anyLong());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void testUpdateInventoryNoChange() {
        InventoryRequestDTO updateDTO = new InventoryRequestDTO(null, null);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        inventoryService.updateInventory(100L, updateDTO);

        verify(productRepository, never()).findById(anyLong());
        verify(entrepotRepository, never()).findById(anyLong());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void testUpdateInventoryNotFound() {
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());
        InventoryRequestDTO updateDTO = new InventoryRequestDTO(2L, 20L);

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.updateInventory(999L, updateDTO));
        assertEquals("Inventaire introuvable avec l’identifiant 999", ex.getMessage());
    }

    @Test
    void testUpdateInventoryProductNotFoundThrowsException() {
        InventoryRequestDTO updateDTO = new InventoryRequestDTO(99L, 10L);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class,
                () -> inventoryService.updateInventory(100L, updateDTO));

        assertEquals("Produit introuvable avec l’identifiant 99", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testUpdateInventoryWarehouseNotFoundThrowsException() {
        InventoryRequestDTO updateDTO = new InventoryRequestDTO(1L, 99L);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(entrepotRepository.findById(99L)).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class,
                () -> inventoryService.updateInventory(100L, updateDTO));

        assertEquals("Entrepôt introuvable avec l’identifiant 99", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testDeleteInventorySuccess() {
        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        doNothing().when(inventoryRepository).delete(inventory);

        inventoryService.deleteInventory(100L);

        verify(inventoryRepository).delete(inventory);
    }

    @Test
    void testDeleteInventoryNotFound() {
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.deleteInventory(999L));
        assertEquals("Inventaire introuvable avec l’identifiant 999", ex.getMessage());
        verify(inventoryRepository, never()).delete(any(Inventory.class));
    }

    @Test
    void testEnregistrerEntreeSuccess() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(100L, 5);
        InventoryMovement movement = new InventoryMovement();

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(mouvementRepository.save(any(InventoryMovement.class))).thenReturn(movement);
        when(mouvementMapper.toDTO(movement)).thenReturn(movementResponseDTO);

        inventoryService.enregistrerEntree(dto);

        assertEquals(55, inventory.getQuantityOnHand()); // 50 + 5
        verify(inventoryRepository).save(inventory);
        verify(mouvementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void testEnregistrerEntreeInvalidQuantity() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(100L, 0);

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.enregistrerEntree(dto));
        assertEquals("La quantité d’entrée doit être supérieure à zéro.", ex.getMessage());
        verify(inventoryRepository, never()).findById(anyLong());
    }

    @Test
    void testEnregistrerEntreeInventoryNotFound() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(999L, 5);

        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.enregistrerEntree(dto));
        assertEquals("Inventaire introuvable.", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testEnregistrerSortieSuccess() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(100L, 10);
        InventoryMovement movement = new InventoryMovement();

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
        when(mouvementRepository.save(any(InventoryMovement.class))).thenReturn(movement);
        when(mouvementMapper.toDTO(movement)).thenReturn(movementResponseDTO);

        inventoryService.enregistrerSortie(dto);

        assertEquals(40, inventory.getQuantityOnHand());
        verify(inventoryRepository).save(inventory);
        verify(mouvementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void testEnregistrerSortieQuantityTooHigh() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(100L, 60);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.enregistrerSortie(dto));
        assertEquals("La quantité demandée dépasse la quantité disponible en stock.", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testEnregistrerSortieInvalidQuantity() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(100L, 0);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.enregistrerSortie(dto));
        assertEquals("La quantité de sortie doit être supérieure à zéro.", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testEnregistrerSortieInventoryNotFound() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(999L, 5);

        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.enregistrerSortie(dto));
        assertEquals("Inventaire introuvable.", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testEnregistrerAjustementPositiveSuccess() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(100L, 5);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));

        when(mouvementRepository.save(any(InventoryMovement.class))).thenReturn(new InventoryMovement());
        when(mouvementMapper.toDTO(any(InventoryMovement.class))).thenReturn(movementResponseDTO);

        inventoryService.enregistrerAjustement(dto);

        assertEquals(55, inventory.getQuantityOnHand());
        verify(inventoryRepository).save(inventory);
        verify(mouvementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void testEnregistrerAjustementNegativeSuccess() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(100L, -5);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));

        when(mouvementRepository.save(any(InventoryMovement.class))).thenReturn(new InventoryMovement());
        when(mouvementMapper.toDTO(any(InventoryMovement.class))).thenReturn(movementResponseDTO);

        inventoryService.enregistrerAjustement(dto);

        assertEquals(45, inventory.getQuantityOnHand());
        verify(inventoryRepository).save(inventory);
        verify(mouvementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void testEnregistrerAjustementNegativeRefused() {
        MovementInventoryRequestDTO dto = new MovementInventoryRequestDTO(100L, -45);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.enregistrerAjustement(dto));
        assertEquals("Ajustement refusé : la quantité disponible ne peut pas être inférieure à la quantité réservée.", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testReserverQuantiteFullReservation() {
        SalesOrderLine line = new SalesOrderLine();
        line.setProduct(product);
        line.setQuantityRequested(40);

        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(inventory));

        inventoryService.reserverQuantite(line, 10L);

        assertEquals(50, inventory.getQuantityReserved());
        assertEquals(40, line.getQuantityReserved());
        assertEquals(0, line.getQuantityBackorder());
        assertEquals(OrderLineStatus.RESERVED, line.getStatus());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void testReserverQuantitePartialReservation() {
        SalesOrderLine line = new SalesOrderLine();
        line.setProduct(product);
        line.setQuantityRequested(60);

        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(inventory));

        inventoryService.reserverQuantite(line, 10L);

        assertEquals(50, inventory.getQuantityReserved());
        assertEquals(40, line.getQuantityReserved());
        assertEquals(20, line.getQuantityBackorder());
        assertEquals(OrderLineStatus.PARTIALLY_RESERVED, line.getStatus());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void testReserverQuantiteNotFound() {
        SalesOrderLine line = new SalesOrderLine();
        line.setProduct(product);
        line.setQuantityRequested(10);

        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.reserverQuantite(line, 10L));
        assertEquals("Aucun inventaire trouvé pour le produit : Laptop", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void testReserverQuantiteZeroAvailable() {
        SalesOrderLine line = new SalesOrderLine();
        line.setProduct(product);
        line.setQuantityRequested(10);

        Inventory noStockInventory = new Inventory();
        noStockInventory.setId(102L);
        noStockInventory.setProduct(product);
        noStockInventory.setWarehouse(warehouse);
        noStockInventory.setQuantityOnHand(50);
        noStockInventory.setQuantityReserved(50);

        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 10L)).thenReturn(Optional.of(noStockInventory));

        inventoryService.reserverQuantite(line, 10L);

        assertEquals(50, noStockInventory.getQuantityReserved());
        assertEquals(0, line.getQuantityReserved());
        assertEquals(10, line.getQuantityBackorder());
        assertEquals(OrderLineStatus.NOT_RESERVED, line.getStatus());
        verify(inventoryRepository).save(noStockInventory);
    }

    @Test
    void testLibererInventaireSuccess() {
        SalesOrderLine line1 = new SalesOrderLine();
        line1.setProduct(product);
        line1.setQuantityReserved(10);

        SalesOrderLine line2 = new SalesOrderLine();
        line2.setProduct(product);
        line2.setQuantityReserved(5);

        SalesOrder order = new SalesOrder();
        order.setWarehouse(warehouse);
        order.setOrderLines(Arrays.asList(line1, line2));

        Inventory inventoryWithReservation = new Inventory();
        inventoryWithReservation.setId(100L);
        inventoryWithReservation.setProduct(product);
        inventoryWithReservation.setWarehouse(warehouse);
        inventoryWithReservation.setQuantityOnHand(50);
        inventoryWithReservation.setQuantityReserved(25);

        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.of(inventoryWithReservation));

        inventoryService.libererInventaire(order);

        assertEquals(10, inventoryWithReservation.getQuantityReserved());
        verify(inventoryRepository, times(2)).save(inventoryWithReservation);
    }

    @Test
    void testLibererInventaireThrowsExceptionIfInventoryNotFound() {
        SalesOrderLine line = new SalesOrderLine();
        line.setProduct(product);
        line.setQuantityReserved(10);

        SalesOrder order = new SalesOrder();
        order.setWarehouse(warehouse);
        order.setOrderLines(List.of(line));

        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 10L))
                .thenReturn(Optional.empty());

        GestionException ex = assertThrows(GestionException.class, () -> inventoryService.libererInventaire(order));
        assertEquals("Aucun inventaire trouvé pour le produit : Laptop", ex.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }
}