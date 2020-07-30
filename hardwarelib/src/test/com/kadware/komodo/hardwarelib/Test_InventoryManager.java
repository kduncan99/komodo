
package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import com.kadware.komodo.hardwarelib.exceptions.ChannelModuleIndexConflictException;
import com.kadware.komodo.hardwarelib.exceptions.DeviceIndexConflictException;
import com.kadware.komodo.hardwarelib.exceptions.InvalidChannelModuleIndexException;
import com.kadware.komodo.hardwarelib.exceptions.InvalidDeviceIndexException;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test_InventoryManager {

    private static InputOutputProcessor _iop0 = null;
    private static InputOutputProcessor _iop1 = null;
    private static InstructionProcessor _ip = null;
    private static MainStorageProcessor _msp = null;
    private static SystemProcessor _sp = null;

    private static ByteChannelModule _cmByte = null;
    private static WordChannelModule _cmWord = null;

    private static FileSystemDiskDevice _fsDisk = null;
    private static ScratchDiskDevice _scDisk = null;
    private static FileSystemTapeDevice _fsTape = null;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  setup and teardown
    //  ----------------------------------------------------------------------------------------------------------------------------

    @BeforeClass
    public static void beforeClass() {
        _iop0 = new InputOutputProcessor("IOP0", InventoryManager.FIRST_IOP_UPI_INDEX);
        _iop1 = new InputOutputProcessor("IOP1", InventoryManager.FIRST_IOP_UPI_INDEX);
        _ip = new InstructionProcessor("IP0", InventoryManager.FIRST_IP_UPI_INDEX);
        _msp = new MainStorageProcessor("MSP0",
                                        InventoryManager.FIRST_MSP_UPI_INDEX,
                                        MainStorageProcessor.MIN_FIXED_SIZE);
        _sp = new SystemProcessor("SP0", 0, 0, null);

        _cmByte = new ByteChannelModule("CMBYTE");
        _cmWord = new WordChannelModule("CMWORD");

        _fsDisk = new FileSystemDiskDevice("FSDISK");
        _fsTape = new FileSystemTapeDevice("TAPE00");
        _scDisk = new ScratchDiskDevice("SCDISK");
    }

    @After
    public void after() {
        InventoryManager.disconnect(_fsDisk);
        InventoryManager.disconnect(_scDisk);
        InventoryManager.disconnect(_fsTape);
        InventoryManager.disconnect(_cmByte);
        InventoryManager.disconnect(_cmWord);
        InventoryManager.disconnect(_iop0);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  success tests
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void chmod_connect_explicit_success(
    ) throws Exception {
        InventoryManager.connect(_iop0, 0, _cmByte);
        InventoryManager.connect(_iop0, 1, _cmWord);

        assertEquals(0, _iop0._ancestors.size());
        assertEquals(2, _iop0._descendants.size());
        assertEquals(1, _cmByte._ancestors.size());
        assertEquals(0, _cmByte._descendants.size());
        assertEquals(1, _cmWord._ancestors.size());
        assertEquals(0, _cmWord._descendants.size());

        assertEquals(_cmByte, _iop0._descendants.get(0));
        assertEquals(_cmWord, _iop0._descendants.get(1));
        assertTrue(_cmByte._ancestors.contains(_iop0));
        assertTrue(_cmWord._ancestors.contains(_iop0));
    }

    @Test
    public void device_connect_explicit_success(
    ) throws Exception {
        InventoryManager.connect(_cmByte, 0, _fsDisk);
        InventoryManager.connect(_cmByte, 1, _scDisk);
        InventoryManager.connect(_cmByte, 2, _fsTape);

        assertEquals(0, _cmByte._ancestors.size());
        assertEquals(3, _cmByte._descendants.size());
        assertEquals(1, _fsDisk._ancestors.size());
        assertEquals(1, _scDisk._ancestors.size());
        assertEquals(1, _fsTape._ancestors.size());

        assertEquals(_fsDisk, _cmByte._descendants.get(0));
        assertEquals(_scDisk, _cmByte._descendants.get(1));
        assertEquals(_fsTape, _cmByte._descendants.get(2));
        assertTrue(_fsDisk._ancestors.contains(_cmByte));
        assertTrue(_scDisk._ancestors.contains(_cmByte));
        assertTrue(_fsTape._ancestors.contains(_cmByte));
    }

    @Test
    public void chmod_connect_implicit_success(
    ) throws Exception {
        InventoryManager.connect(_iop0, _cmByte);
        InventoryManager.connect(_iop0, _cmWord);

        assertEquals(0, _iop0._ancestors.size());
        assertEquals(2, _iop0._descendants.size());
        assertEquals(1, _cmByte._ancestors.size());
        assertEquals(0, _cmByte._descendants.size());
        assertEquals(1, _cmWord._ancestors.size());
        assertEquals(0, _cmWord._descendants.size());

        assertEquals(_cmByte, _iop0._descendants.get(0));
        assertEquals(_cmWord, _iop0._descendants.get(1));
        assertTrue(_cmByte._ancestors.contains(_iop0));
        assertTrue(_cmWord._ancestors.contains(_iop0));
    }

    @Test
    public void device_connect_implicit_success(
    ) throws Exception {
        InventoryManager.connect(_cmByte, _fsDisk);
        InventoryManager.connect(_cmByte, _scDisk);
        InventoryManager.connect(_cmByte, _fsTape);

        assertEquals(0, _cmByte._ancestors.size());
        assertEquals(3, _cmByte._descendants.size());
        assertEquals(1, _fsDisk._ancestors.size());
        assertEquals(1, _scDisk._ancestors.size());
        assertEquals(1, _fsTape._ancestors.size());

        assertEquals(_fsDisk, _cmByte._descendants.get(0));
        assertEquals(_scDisk, _cmByte._descendants.get(1));
        assertEquals(_fsTape, _cmByte._descendants.get(2));
        assertTrue(_fsDisk._ancestors.contains(_cmByte));
        assertTrue(_scDisk._ancestors.contains(_cmByte));
        assertTrue(_fsTape._ancestors.contains(_cmByte));
    }

    @Test
    public void disconnect_all(
    ) throws Exception {
        InventoryManager.connect(_iop0, _cmByte);
        InventoryManager.connect(_cmByte, _fsDisk);
        InventoryManager.connect(_cmByte, _fsTape);
        InventoryManager.disconnect(_cmByte);

        assertEquals(0, _iop0._descendants.size());
        assertEquals(0, _cmByte._ancestors.size());
        assertEquals(0, _cmByte._descendants.size());
        assertEquals(0, _fsDisk._ancestors.size());
        assertEquals(0, _fsTape._ancestors.size());
    }

    @Test
    public void disconnect_ancestors(
    ) throws Exception {
        InventoryManager.connect(_iop0, _cmByte);
        InventoryManager.connect(_cmByte, _fsDisk);
        InventoryManager.connect(_cmByte, _fsTape);
        InventoryManager.disconnectAncestors(_cmByte);

        assertEquals(0, _iop0._descendants.size());
        assertEquals(0, _cmByte._ancestors.size());
        assertEquals(2, _cmByte._descendants.size());
        assertEquals(1, _fsDisk._ancestors.size());
        assertEquals(1, _fsTape._ancestors.size());
    }

    @Test
    public void disconnect_descendants(
    ) throws Exception {
        InventoryManager.connect(_iop0, _cmByte);
        InventoryManager.connect(_cmByte, _fsDisk);
        InventoryManager.connect(_cmByte, _fsTape);
        InventoryManager.disconnectDescendants(_cmByte);

        assertEquals(1, _iop0._descendants.size());
        assertEquals(1, _cmByte._ancestors.size());
        assertEquals(0, _cmByte._descendants.size());
        assertEquals(0, _fsDisk._ancestors.size());
        assertEquals(0, _fsTape._ancestors.size());
    }

    @Test
    public void disconnect_not_connected() {
        //  should be no exception, as this is normal
        InventoryManager.disconnect(_iop0);
    }

    @Test
    public void disconnect_specific(
    ) throws Exception {
        InventoryManager.connect(_iop0, _cmByte);
        InventoryManager.connect(_cmByte, _fsDisk);
        InventoryManager.connect(_cmByte, _fsTape);
        InventoryManager.disconnect(_cmByte, _fsDisk);

        assertEquals(1, _iop0._descendants.size());
        assertEquals(1, _cmByte._ancestors.size());
        assertEquals(1, _cmByte._descendants.size());
        assertEquals(0, _fsDisk._ancestors.size());
        assertEquals(1, _fsTape._ancestors.size());
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  exception tests
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Test
    public void msps_cannot_be_descendants_of_anything() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _msp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop0, _msp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _msp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _msp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmByte, _msp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _msp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _msp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _msp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _msp));
    }

    @Test
    public void iops_cannot_be_descendants_of_anything() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _iop0));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop0, _iop0));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _iop0));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _iop0));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmByte, _iop0));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _iop0));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _iop0));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _iop0));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _iop0));
    }

    @Test
    public void ips_cannot_be_descendants_of_anything() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _ip));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop0, _ip));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _ip));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _ip));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmByte, _ip));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _ip));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _ip));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _ip));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _ip));
    }

    @Test
    public void sps_cannot_be_descendants_of_anything() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _sp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop0, _sp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _sp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _sp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmByte, _sp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _sp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _sp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _sp));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _sp));
    }

    @Test
    public void cmByte_connect_fail() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _cmByte));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _cmByte));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _cmByte));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmByte, _cmByte));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _cmByte));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _cmByte));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _cmByte));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _cmByte));
    }

    @Test
    public void cmWord_connect_fail() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _cmWord));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _cmWord));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _cmWord));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmByte, _cmWord));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _cmWord));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _cmWord));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _cmWord));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _cmWord));
    }

    @Test
    public void fsDisk_connect_fail() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _fsDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop0, _fsDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _fsDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _fsDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _fsDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _fsDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _fsDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _fsDisk));
    }

    @Test
    public void scDisk_connect_fail() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _scDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop0, _scDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _scDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _scDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _scDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _scDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _scDisk));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _scDisk));
    }

    @Test
    public void fsTape_connect_fail() {
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_msp, _fsTape));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop0, _fsTape));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_ip, _fsTape));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_sp, _fsTape));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmWord, _fsTape));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsDisk, _fsTape));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_fsTape, _fsTape));
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_scDisk, _fsTape));
    }

    @Test
    public void chmod_index_conflict() throws Exception {
        InventoryManager.connect(_iop0, 0, _cmByte);
        assertThrows(ChannelModuleIndexConflictException.class, () -> InventoryManager.connect(_iop0, 0, _cmWord));
    }

    @Test
    public void chmod_cannot_connect_already_connected_1() throws Exception {
        InventoryManager.connect(_iop0, 0, _cmByte);
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop0, 1, _cmByte));
    }

    @Test
    public void chmod_cannot_connect_already_connected_2() throws Exception {
        InventoryManager.connect(_iop0, 0, _cmByte);
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_iop1, 0, _cmByte));
    }

    @Test
    public void chmod_invalid_index() {
        assertThrows(InvalidChannelModuleIndexException.class, () ->
            InventoryManager.connect(_iop0, -1, _cmByte));
        assertThrows(InvalidChannelModuleIndexException.class, () ->
            InventoryManager.connect(_iop0, InventoryManager.LAST_CHANNEL_MODULE_INDEX + 1, _cmByte));
    }

    @Test
    public void chmod_max_per_iop_implicit(
    ) throws Exception {
        for (int x = 0; x <= InventoryManager.LAST_CHANNEL_MODULE_INDEX; ++x) {
            String devName = "CM" + x;
            InventoryManager.connect(_iop0, new ByteChannelModule(devName));
        }

        assertThrows(InvalidChannelModuleIndexException.class, () ->
            InventoryManager.connect(_iop0, new ByteChannelModule("CMBAD")));
    }

    @Test
    public void device_index_conflict(
    ) throws Exception {
        InventoryManager.connect(_cmByte, 0, _fsDisk);
        assertThrows(DeviceIndexConflictException.class, () -> InventoryManager.connect(_cmByte, 0, _fsTape));
    }

    @Test
    public void device_cannot_connect_already_connected(
    ) throws Exception {
        InventoryManager.connect(_cmByte, 0, _fsDisk);
        assertThrows(CannotConnectException.class, () -> InventoryManager.connect(_cmByte, 1, _fsDisk));
    }

    @Test
    public void device_invalid_index() {
        assertThrows(InvalidDeviceIndexException.class, () ->
            InventoryManager.connect(_cmByte, -1, _fsDisk));
        assertThrows(InvalidDeviceIndexException.class, () ->
            InventoryManager.connect(_cmByte, InventoryManager.LAST_DEVICE_INDEX + 1, _fsDisk));
    }

    @Test
    public void device_max_per_chmod_implicit(
    ) throws Exception {
        for (int x = 0; x <= InventoryManager.LAST_DEVICE_INDEX; ++x) {
            String devName = "FSDD" + x;
            InventoryManager.connect(_cmByte, new FileSystemDiskDevice(devName));
        }

        assertThrows(InvalidDeviceIndexException.class, () ->
            InventoryManager.connect(_cmByte, new FileSystemDiskDevice("DDBAD")));
    }
}
