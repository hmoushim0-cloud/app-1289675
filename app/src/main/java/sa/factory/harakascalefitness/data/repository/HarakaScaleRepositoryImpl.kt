package sa.factory.harakascalefitness.data.repository

import sa.factory.harakascalefitness.domain.repository.HarakaScaleRepository
import sa.factory.harakascalefitness.hardware.HardwareSensorDriver
import sa.factory.harakascalefitness.model.HarakaScaleItem
import sa.factory.harakascalefitness.model.HarakaScaleOrder
import sa.factory.harakascalefitness.model.HarakaScaleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HarakaScaleRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : HarakaScaleRepository {

    private val _catalog = MutableStateFlow<List<HarakaScaleItem>>(listOf(
        HarakaScaleItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع BLE Smart Scale GATT 0x181D + Heart Rate Strap", 180.0, "معتمد", "الخدمات الرئيسية"),
        HarakaScaleItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        HarakaScaleItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<HarakaScaleItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<HarakaScaleItem>>(emptyList())
    override val activeCart: StateFlow<List<HarakaScaleItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<HarakaScaleTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            HarakaScaleTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: HarakaScaleItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: HarakaScaleItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): HarakaScaleOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = HarakaScaleOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
