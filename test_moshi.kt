import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FuncCall(
    val name: String,
    val args: Map<String, Any>? = null
)

fun main() {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(FuncCall::class.java)
    val json = """{"name": "create_file", "args": {"path": "test.txt", "content": "hello"}}"""
    val obj = adapter.fromJson(json)
    println(obj)
}
