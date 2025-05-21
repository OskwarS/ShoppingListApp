package com.example.shoppinglistapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID


data class Product(
    val id: String,
    val name: String,
    val quantity: Int,
    var isPurchased: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoppingListApp()
        }
    }
}

@Composable
fun ShoppingListApp(viewModel: ShoppingListViewModel = viewModel() ) {
    val productList by viewModel.productList.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            ComposableShoppingList(
                productList = productList,
                onAddProduct = viewModel::addProduct,
                onPurchaseProduct = viewModel::toggleProductPurchased,
                onDeleteProduct = viewModel::deleteProduct
            )
        }
    }
}

@Composable
fun ComposableShoppingList(
    productList: List<Product>,
    onAddProduct: (String, Int) -> Unit,
    onPurchaseProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var productName by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Product")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add product to list") },
            text = {
                Column {
                    TextField(value = productName, onValueChange = { productName = it }, label = { Text("Product Name") })
                    TextField(value = productQuantity, onValueChange = { productQuantity = it }, label = { Text("Quantity") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (productName.isNotBlank() && productQuantity.isNotBlank()) {
                            onAddProduct(productName, productQuantity.toIntOrNull() ?: 1)
                            showDialog = false
                            productName = ""
                            productQuantity = ""
                        }
                    }
                ) {
                    Text("ADD")
                }
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(productList) { product ->
            ProductRow(
                product = product,
                onPurchaseClick = { onPurchaseProduct(product) },
                onDeleteClick = { onDeleteProduct(product) }
            )
        }
    }
}

@Composable
fun ProductRow(
    product: Product,
    onPurchaseClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = product.name, modifier = Modifier.weight(1f))
        Text(text = "Qty: ${product.quantity}", modifier = Modifier.weight(0.5f))

        IconButton(onClick = onPurchaseClick) {
            Icon(imageVector = if (product.isPurchased) Icons.Default.Check else Icons.Default.Clear, contentDescription = "Purchase Icon")
        }
        IconButton(onClick = onDeleteClick) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Icon")
        }
    }
}

class ShoppingListViewModel(private val context: Context) : ViewModel() {
    private val sharedPreferences = context.getSharedPreferences("list_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _productList = MutableStateFlow<List<Product>>(loadProductList())
    val productList: StateFlow<List<Product>> = _productList

    fun addProduct(name: String, quantity: Int) {
        val newProduct = Product(
            id = UUID.randomUUID().toString(),
            name = name,
            quantity = quantity
        )
        _productList.value = _productList.value + newProduct
        saveProductList()
    }

    fun toggleProductPurchased(product: Product) {
        _productList.value = _productList.value.map {
            if (it.id == product.id) it.copy(isPurchased = !it.isPurchased) else it
        }
        saveProductList()
    }

    fun deleteProduct(product: Product) {
        _productList.value = _productList.value - product
        saveProductList()
    }

    private fun saveProductList() {
        val editor = sharedPreferences.edit()
        val jsonString = gson.toJson(_productList.value)
        editor.putString("product_list", jsonString).apply()
    }

    private fun loadProductList(): List<Product> {
        val jsonString = sharedPreferences.getString("product_list", null) ?: return emptyList()
        return gson.fromJson(jsonString, object : com.google.gson.reflect.TypeToken<List<Product>>() {}.type)
    }
}
