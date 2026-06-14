package com.example.shizukuproxy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.DataOutputStream

data class ConnectedDevice(
    val ip: String,
    val mac: String,
    val interfaceName: String,
    val status: String,
    val manufacturer: String
)

/**
 * Handles privileged shell queries via Shizuku binder layer.
 * Specifically aims to locate the softAP interface gateway.
 */
object ShizukuShellExecutor {

    private const val TAG = "ShizukuShellExecutor"

    private val MAC_OUI_MAP = mapOf(
        "00000C" to "Cisco Systems",
        "000142" to "Cisco Systems",
        "00037F" to "Atheros Communications",
        "0003FF" to "Microsoft Corporation",
        "000420" to "Intel Corporation",
        "0005B5" to "Broadcom Inc.",
        "000C29" to "VMware, Inc.",
        "001122" to "CUI Inc.",
        "001132" to "Synology Incorporated",
        "0011D8" to "ASUSTek Computer Inc.",
        "001377" to "Xiaomi Communications",
        "00155D" to "Microsoft Corporation",
        "00163E" to "XenSource",
        "0017F2" to "Apple Inc.",
        "001A11" to "Google LLC",
        "001A8C" to "Sophos Ltd.",
        "001C42" to "Parallels",
        "001E8C" to "ASUSTek Computer Inc.",
        "00215F" to "Cisco Systems",
        "002170" to "Apple Inc.",
        "002241" to "Apple Inc.",
        "002332" to "Apple Inc.",
        "002443" to "Apple Inc.",
        "002500" to "Apple Inc.",
        "002590" to "Super Micro Computer, Inc.",
        "0026BB" to "Apple Inc.",
        "003095" to "Sony Corporation",
        "0040A6" to "Cisco Systems",
        "005056" to "VMware, Inc.",
        "0090A9" to "Western Digital Technologies",
        "00A0C9" to "Intel Corporation",
        "00A0CC" to "Lite-On Technology",
        "00CDFE" to "Apple Inc.",
        "00E04C" to "Realtek Semiconductor",
        "0418D6" to "Ubiquiti Networks",
        "04D4C4" to "Samsung Electronics",
        "04D9F5" to "ASUSTek Computer Inc.",
        "04F0E8" to "Apple Inc.",
        "0C8B7D" to "Huawei Technologies",
        "0CD996" to "OnePlus Technology",
        "106FD9" to "Apple Inc.",
        "10B96B" to "Apple Inc.",
        "14201B" to "OnePlus Technology",
        "145F85" to "Samsung Electronics",
        "147DD7" to "Xiaomi Communications",
        "149182" to "Belkin International",
        "18012C" to "Xiaomi Communications",
        "181EC0" to "Samsung Electronics",
        "182A7B" to "Apple Inc.",
        "183425" to "Samsung Electronics",
        "186590" to "Apple Inc.",
        "1867B0" to "Samsung Electronics",
        "203706" to "Xiaomi Communications",
        "2082C0" to "Apple Inc.",
        "20A60C" to "Apple Inc.",
        "2400BA" to "Samsung Electronics",
        "244B03" to "Samsung Electronics",
        "24A074" to "Apple Inc.",
        "24AB81" to "Google LLC",
        "286B35" to "Apple Inc.",
        "28C2DD" to "Samsung Electronics",
        "2C088C" to "Samsung Electronics",
        "2C26C5" to "Apple Inc.",
        "2C2B97" to "Samsung Electronics",
        "2C4D54" to "Apple Inc.",
        "2C5491" to "Huawei Technologies",
        "2C5A0F" to "Sony Corporation",
        "2CF0A2" to "Synology Incorporated",
        "30074D" to "Xiaomi Communications",
        "30144A" to "Apple Inc.",
        "3035AD" to "Samsung Electronics",
        "305A3A" to "Google LLC",
        "3085A9" to "ASUSTek Computer Inc.",
        "3085C9" to "Intel Corporation",
        "30B5C2" to "Samsung Electronics",
        "34159E" to "Apple Inc.",
        "342E37" to "Xiaomi Communications",
        "344262" to "Samsung Electronics",
        "344B50" to "Apple Inc.",
        "34A8EB" to "Samsung Electronics",
        "380195" to "Apple Inc.",
        "3818FD" to "Samsung Electronics",
        "382C4A" to "Xiaomi Communications",
        "387858" to "Apple Inc.",
        "3C15C2" to "Samsung Electronics",
        "3C5AB4" to "Google LLC",
        "3C7A11" to "Apple Inc.",
        "3C8C93" to "Xiaomi Communications",
        "3C9509" to "Huawei Technologies",
        "3CA067" to "Apple Inc.",
        "3CD0F8" to "Samsung Electronics",
        "40223F" to "Apple Inc.",
        "4035C9" to "Samsung Electronics",
        "404D7F" to "Apple Inc.",
        "404E36" to "HTC Corporation",
        "408338" to "Apple Inc.",
        "408805" to "Samsung Electronics",
        "40A36C" to "Apple Inc.",
        "40B6B1" to "Samsung Electronics",
        "440010" to "Google LLC",
        "44237C" to "Xiaomi Communications",
        "444C0C" to "Apple Inc.",
        "446132" to "Samsung Electronics",
        "44650D" to "Google LLC",
        "448500" to "Apple Inc.",
        "482C6A" to "Samsung Electronics",
        "4843A9" to "Apple Inc.",
        "484D3E" to "Xiaomi Communications",
        "485073" to "Samsung Electronics",
        "4851B7" to "Huawei Technologies",
        "485B39" to "Apple Inc.",
        "48A917" to "Apple Inc.",
        "48D63D" to "Samsung Electronics",
        "4C11AE" to "Samsung Electronics",
        "4C3275" to "Apple Inc.",
        "4C3488" to "Intel Corporation",
        "4C4E35" to "Samsung Electronics",
        "4C57CA" to "Apple Inc.",
        "4C74BF" to "Google LLC",
        "4C7C5F" to "Apple Inc.",
        "4CAF75" to "OnePlus Technology",
        "503275" to "Samsung Electronics",
        "508A06" to "Apple Inc.",
        "50C7BF" to "TP-Link Technologies",
        "50EC50" to "Samsung Electronics",
        "54271E" to "Xiaomi Communications",
        "542A1B" to "Apple Inc.",
        "543A3E" to "Samsung Electronics",
        "54446A" to "Apple Inc.",
        "546009" to "Google LLC",
        "54724F" to "Samsung Electronics",
        "549963" to "Apple Inc.",
        "54E43A" to "Apple Inc.",
        "5C514F" to "Apple Inc.",
        "5C5B35" to "Samsung Electronics",
        "5C93A2" to "Xiaomi Communications",
        "5C969D" to "Apple Inc.",
        "5CE0C5" to "Samsung Electronics",
        "5CE931" to "Samsung Electronics",
        "600308" to "Apple Inc.",
        "6014B3" to "Samsung Electronics",
        "606944" to "Xiaomi Communications",
        "60830A" to "Apple Inc.",
        "60B4F7" to "Samsung Electronics",
        "60D819" to "Apple Inc.",
        "60E327" to "Apple Inc.",
        "60ECA8" to "Samsung Electronics",
        "640980" to "Apple Inc.",
        "641C67" to "Samsung Electronics",
        "645D86" to "Samsung Electronics",
        "6466B3" to "Xiaomi Communications",
        "647033" to "Apple Inc.",
        "6489F1" to "Samsung Electronics",
        "64A769" to "Apple Inc.",
        "64B5C6" to "Samsung Electronics",
        "64BC58" to "Samsung Electronics",
        "68224B" to "Samsung Electronics",
        "682F67" to "Samsung Electronics",
        "683E11" to "Apple Inc.",
        "68541D" to "Samsung Electronics",
        "685B35" to "Apple Inc.",
        "68A86D" to "Apple Inc.",
        "6C19C0" to "Apple Inc.",
        "6C25B9" to "Samsung Electronics",
        "6C4008" to "Apple Inc.",
        "6C5D3A" to "Samsung Electronics",
        "6C7660" to "Apple Inc.",
        "6C8D11" to "Apple Inc.",
        "6C96CF" to "Samsung Electronics",
        "702C1F" to "Apple Inc.",
        "703EAC" to "Apple Inc.",
        "70480F" to "Apple Inc.",
        "7081EB" to "Samsung Electronics",
        "7085C6" to "ASUSTek Computer Inc.",
        "70B317" to "Samsung Electronics",
        "70B3D5" to "Google LLC",
        "70D4F0" to "Xiaomi Communications",
        "7413FF" to "Samsung Electronics",
        "742344" to "Samsung Electronics",
        "745E1C" to "Samsung Electronics",
        "747548" to "Apple Inc.",
        "74AC5F" to "Samsung Electronics",
        "74A722" to "Apple Inc.",
        "74B587" to "Apple Inc.",
        "74C63B" to "Samsung Electronics",
        "74E50B" to "Samsung Electronics",
        "781881" to "Apple Inc.",
        "7831C1" to "Apple Inc.",
        "78471D" to "Samsung Electronics",
        "784F43" to "Samsung Electronics",
        "787B8A" to "Apple Inc.",
        "78843C" to "Apple Inc.",
        "78A106" to "Apple Inc.",
        "78C1A7" to "Samsung Electronics",
        "78D6F0" to "Apple Inc.",
        "78E7D1" to "Samsung Electronics",
        "7C010A" to "Apple Inc.",
        "7C11CB" to "Samsung Electronics",
        "7C6193" to "Samsung Electronics",
        "7C5079" to "Xiaomi Communications",
        "7C6D62" to "Apple Inc.",
        "7C7D3D" to "OnePlus Technology",
        "7CD1C3" to "Apple Inc.",
        "801967" to "Sony Corporation",
        "804CF8" to "Xiaomi Communications",
        "804E81" to "Samsung Electronics",
        "841766" to "Samsung Electronics",
        "842999" to "Apple Inc.",
        "843835" to "Apple Inc.",
        "8478AC" to "Apple Inc.",
        "84852C" to "Samsung Electronics",
        "848E0C" to "Xiaomi Communications",
        "84A466" to "Samsung Electronics",
        "84D6F7" to "Apple Inc.",
        "84DBAC" to "Apple Inc.",
        "84F3EB" to "Samsung Electronics",
        "84FCAC" to "Apple Inc.",
        "880F10" to "Samsung Electronics",
        "881908" to "Apple Inc.",
        "88308A" to "Samsung Electronics",
        "884477" to "Apple Inc.",
        "885395" to "Apple Inc.",
        "8862F5" to "Apple Inc.",
        "88665A" to "Apple Inc.",
        "887556" to "Samsung Electronics",
        "88AE07" to "Apple Inc.",
        "88C1A0" to "Samsung Electronics",
        "88C94B" to "Xiaomi Communications",
        "88E9FE" to "Apple Inc.",
        "8C2937" to "Samsung Electronics",
        "8C3A30" to "Xiaomi Communications",
        "8C58BC" to "Apple Inc.",
        "8C646F" to "Samson Tech",
        "8C7967" to "Apple Inc.",
        "8C8401" to "Samsung Electronics",
        "8C8590" to "Apple Inc.",
        "8C982D" to "Samsung Electronics",
        "8CA982" to "Apple Inc.",
        "8CE10F" to "HP Inc.",
        "8CF2FB" to "Apple Inc.",
        "900628" to "Apple Inc.",
        "90185F" to "Samsung Electronics",
        "901D27" to "Apple Inc.",
        "903271" to "Apple Inc.",
        "9060E1" to "Samsung Electronics",
        "90680A" to "Apple Inc.",
        "907240" to "Apple Inc.",
        "90735A" to "Samsung Electronics",
        "907F61" to "Apple Inc.",
        "908D6C" to "Apple Inc.",
        "90B686" to "Samsung Electronics",
        "90DD5D" to "Samsung Electronics",
        "90E17B" to "Apple Inc.",
        "90F0AA" to "Apple Inc.",
        "94103F" to "Apple Inc.",
        "941625" to "Samsung Electronics",
        "943C21" to "OnePlus Technology",
        "94652D" to "Samsung Electronics",
        "9476B7" to "Xiaomi Communications",
        "94E979" to "Apple Inc.",
        "94EA07" to "Apple Inc.",
        "94F65B" to "Samsung Electronics",
        "9801A7" to "Apple Inc.",
        "980D2E" to "Samsung Electronics",
        "9810E8" to "Apple Inc.",
        "98398E" to "Samsung Electronics",
        "984B4A" to "Samsung Electronics",
        "9852B1" to "Apple Inc.",
        "989E63" to "Apple Inc.",
        "98B5F5" to "Samsung Electronics",
        "98D6F7" to "Samsung Electronics",
        "98E7F4" to "Apple Inc.",
        "98E7F5" to "Apple Inc.",
        "9C1125" to "Xiaomi Communications",
        "9C2EA1" to "Samsung Electronics",
        "9C305B" to "Apple Inc.",
        "9C3215" to "Samsung Electronics",
        "9C341A" to "Samsung Electronics",
        "9C4FDA" to "Apple Inc.",
        "9C5C8E" to "Samsung Electronics",
        "9C8D7C" to "Apple Inc.",
        "9CE6E3" to "Samsung Electronics",
        "9CE6E7" to "Samsung Electronics",
        "9CF387" to "Apple Inc.",
        "A02E17" to "Apple Inc.",
        "A056B2" to "Samsung Electronics",
        "A07A8F" to "Apple Inc.",
        "A0999B" to "Apple Inc.",
        "A0A8CD" to "Samsung Electronics",
        "A0C589" to "Xiaomi Communications",
        "A0E70A" to "Apple Inc.",
        "A43135" to "Samsung Electronics",
        "A45E60" to "Apple Inc.",
        "A470D6" to "Samsung Electronics",
        "A47733" to "Apple Inc.",
        "A47055" to "Samsung Electronics",
        "A4B197" to "Apple Inc.",
        "A4C737" to "Samsung Electronics",
        "A4CF12" to "Google LLC",
        "A4D9AA" to "Samsung Electronics",
        "A4E975" to "Apple Inc.",
        "A4FC77" to "Samsung Electronics",
        "A82B12" to "Samsung Electronics",
        "A83B76" to "Samsung Electronics",
        "A85B78" to "Apple Inc.",
        "A8667F" to "Apple Inc.",
        "A879A2" to "Apple Inc.",
        "A88E24" to "Apple Inc.",
        "A8968A" to "Apple Inc.",
        "A89C12" to "Samsung Electronics",
        "A8A795" to "Apple Inc.",
        "A8C83F" to "Samsung Electronics",
        "A8C87D" to "Samsung Electronics",
        "A8DAC2" to "Apple Inc.",
        "A8E3EE" to "Samsung Electronics",
        "A8FA48" to "Samsung Electronics",
        "AC1D06" to "Apple Inc.",
        "AC2B6B" to "Samsung Electronics",
        "AC3B77" to "Apple Inc.",
        "AC5F3E" to "Samsung Electronics",
        "AC7BA1" to "Apple Inc.",
        "AC87A3" to "Apple Inc.",
        "ACC309" to "Samsung Electronics",
        "ACC8E6" to "Apple Inc.",
        "B019C5" to "Samsung Electronics",
        "B0359F" to "Google LLC",
        "B0394F" to "Realtek Semiconductor",
        "B03956" to "Apple Inc.",
        "B04519" to "Apple Inc.",
        "B0702D" to "Samsung Electronics",
        "B072BF" to "Apple Inc.",
        "B09122" to "Samsung Electronics",
        "B0C4E7" to "Samsung Electronics",
        "B0C62F" to "Apple Inc.",
        "B0D59D" to "Samsung Electronics",
        "B40016" to "Infinix Mobility",
        "B418A3" to "Samsung Electronics",
        "B4527D" to "Apple Inc.",
        "B47443" to "Samsung Electronics",
        "B4791E" to "Apple Inc.",
        "B4F1EB" to "Apple Inc.",
        "B4F7A1" to "Samsung Electronics",
        "B8098A" to "Samsung Electronics",
        "B81D0A" to "Apple Inc.",
        "B827EB" to "Raspberry Pi Foundation",
        "B83765" to "Apple Inc.",
        "B853AC" to "Apple Inc.",
        "B85D0A" to "Samsung Electronics",
        "B863BC" to "Apple Inc.",
        "B8782E" to "Apple Inc.",
        "B87C2F" to "Samsung Electronics",
        "B88198" to "Samsung Electronics",
        "B8A175" to "Apple Inc.",
        "B8B822" to "Samsung Electronics",
        "B8BC27" to "Apple Inc.",
        "B8C75D" to "Apple Inc.",
        "B8E856" to "Apple Inc.",
        "B8EC11" to "Samsung Electronics",
        "B8F6B1" to "Samsung Electronics",
        "BC1485" to "Samsung Electronics",
        "BC4CC4" to "Apple Inc.",
        "BC5436" to "Samsung Electronics",
        "BC8385" to "Samsung Electronics",
        "BC98DF" to "Apple Inc.",
        "BC9FEE" to "Apple Inc.",
        "BCA8A6" to "Apple Inc.",
        "BCDC3B" to "Samsung Electronics",
        "BCE59F" to "Samsung Electronics",
        "C00415" to "Xiaomi Communications",
        "C00A95" to "Apple Inc.",
        "C01A9C" to "Apple Inc.",
        "C03896" to "Samsung Electronics",
        "C045C3" to "Samsung Electronics",
        "C04A00" to "Samsung Electronics",
        "C0847A" to "Apple Inc.",
        "C09A3D" to "Apple Inc.",
        "C0A5D1" to "Apple Inc.",
        "C0A600" to "Samsung Electronics",
        "C0B3B0" to "Samsung Electronics",
        "C0D2F4" to "Xiaomi Communications",
        "C0D7AA" to "Apple Inc.",
        "C0EEFB" to "Apple Inc.",
        "C0F2CB" to "Samsung Electronics",
        "C40528" to "Samsung Electronics",
        "C43A35" to "Apple Inc.",
        "C44EAC" to "Apple Inc.",
        "C47D4F" to "Samsung Electronics",
        "C49313" to "Samsung Electronics",
        "C49880" to "Apple Inc.",
        "C49F34" to "Samsung Electronics",
        "C4D015" to "Apple Inc.",
        "C4E13F" to "Apple Inc.",
        "C802A6" to "Samsung Electronics",
        "C81E3B" to "Apple Inc.",
        "C82158" to "Fuzhou Rockchip Electronics",
        "C83A35" to "Apple Inc.",
        "C863B5" to "Apple Inc.",
        "C869CD" to "Apple Inc.",
        "C86B14" to "Samsung Electronics",
        "C88D83" to "Samsung Electronics",
        "C88FCA" to "Samsung Electronics",
        "C8979F" to "Samsung Electronics",
        "C8D3A3" to "Apple Inc.",
        "C8D7B0" to "Samsung Electronics",
        "C8E0EB" to "Samsung Electronics",
        "C8EBB0" to "Samsung Electronics",
        "C8F733" to "Apple Inc.",
        "CC088D" to "Samsung Electronics",
        "CC20E8" to "Apple Inc.",
        "CC25EF" to "Apple Inc.",
        "CC29F5" to "Samsung Electronics",
        "CC3BE5" to "Apple Inc.",
        "CC4463" to "Samsung Electronics",
        "CC749A" to "Apple Inc.",
        "CCA223" to "Samsung Electronics",
        "CCB255" to "Apple Inc.",
        "CCC3EA" to "Apple Inc.",
        "CCC5A8" to "Samsung Electronics",
        "CCE613" to "Samsung Electronics",
        "CCF2D5" to "Samsung Electronics",
        "CCF315" to "Samsung Electronics",
        "CDFE00" to "Apple Inc.",
        "D0034B" to "Apple Inc.",
        "D012E4" to "Apple Inc.",
        "D017C2" to "Samsung Electronics",
        "D022BE" to "Apple Inc.",
        "D03311" to "Apple Inc.",
        "D03742" to "Huawei Technologies",
        "D05162" to "Apple Inc.",
        "D059E4" to "Apple Inc.",
        "D07714" to "Samsung Electronics",
        "D087E2" to "Apple Inc.",
        "D0D2B0" to "Samsung Electronics",
        "D0FEE0" to "Samsung Electronics",
        "D43806" to "Samsung Electronics",
        "D4619D" to "Samsung Electronics",
        "D41C61" to "Apple Inc.",
        "D490F0" to "Apple Inc.",
        "D4A148" to "Apple Inc.",
        "D4DC07" to "Apple Inc.",
        "D4F46F" to "Apple Inc.",
        "D8004D" to "Samsung Electronics",
        "D80F99" to "Samsung Electronics",
        "D81C58" to "Apple Inc.",
        "D83062" to "Apple Inc.",
        "D83125" to "Samsung Electronics",
        "D857EF" to "Samsung Electronics",
        "D88F76" to "Apple Inc.",
        "D89695" to "Apple Inc.",
        "D89EF3" to "Apple Inc.",
        "D8A25E" to "Apple Inc.",
        "D8B190" to "Samsung Electronics",
        "D8B2C4" to "Xiaomi Communications",
        "D8C467" to "Samsung Electronics",
        "D8D1CB" to "Samsung Electronics",
        "DC2B2A" to "Apple Inc.",
        "DC4ACC" to "Apple Inc.",
        "DC0C5C" to "Apple Inc.",
        "DC52D0" to "Samsung Electronics",
        "DC7160" to "Samsung Electronics",
        "DC729B" to "Apple Inc.",
        "DC8A24" to "Apple Inc.",
        "DCA904" to "Apple Inc.",
        "DCD916" to "Samsung Electronics",
        "DCEFD0" to "Samsung Electronics",
        "E0115F" to "Samsung Electronics",
        "E01A8F" to "Samsung Electronics",
        "E0247F" to "Apple Inc.",
        "E043DB" to "Apple Inc.",
        "E06678" to "Samsung Electronics",
        "E0AA96" to "Apple Inc.",
        "E0C9B1" to "Apple Inc.",
        "E0D55E" to "Samsung Electronics",
        "E0F847" to "Apple Inc.",
        "E40439" to "Apple Inc.",
        "E41218" to "Samsung Electronics",
        "E425E9" to "Samsung Electronics",
        "E432CB" to "Apple Inc.",
        "E450E6" to "Samsung Electronics",
        "E458B8" to "Apple Inc.",
        "E47D25" to "Apple Inc.",
        "E48B7F" to "Apple Inc.",
        "E4907E" to "Samsung Electronics",
        "E4A1D9" to "Apple Inc.",
        "E4AA5D" to "Apple Inc.",
        "E4B202" to "Samsung Electronics",
        "E4C14C" to "Samsung Electronics",
        "E4E4C6" to "Samsung Electronics",
        "E4E749" to "Samsung Electronics",
        "E8039A" to "Apple Inc.",
        "E8040B" to "Samsung Electronics",
        "E80688" to "Apple Inc.",
        "E84E06" to "Samsung Electronics",
        "E8507B" to "Samsung Electronics",
        "E86D52" to "Apple Inc.",
        "E8802E" to "Apple Inc.",
        "E88D28" to "Apple Inc.",
        "E8A4C1" to "Samsung Electronics",
        "E8BBA8" to "Samsung Electronics",
        "E8D819" to "Apple Inc.",
        "E8DEC5" to "Samsung Electronics",
        "ECE555" to "Samsung Electronics",
        "ECE561" to "Samsung Electronics",
        "ECEB6E" to "Samsung Electronics",
        "ECF342" to "Apple Inc.",
        "F0143D" to "Samsung Electronics",
        "F01898" to "Apple Inc.",
        "F02408" to "Samsung Electronics",
        "F05A09" to "Apple Inc.",
        "F07597" to "Apple Inc.",
        "F07960" to "Samsung Electronics",
        "F099BF" to "Apple Inc.",
        "F0A44E" to "Samsung Electronics",
        "F0C1F1" to "Google LLC",
        "F0D5BF" to "Samsung Electronics",
        "F0D7E1" to "Apple Inc.",
        "F0DBF8" to "Apple Inc.",
        "F0F55A" to "Samsung Electronics",
        "F40F24" to "Apple Inc.",
        "F41B5F" to "Samsung Electronics",
        "F45C89" to "Samsung Electronics",
        "F45EAB" to "Apple Inc.",
        "F460E2" to "Samsung Electronics",
        "F4B7E2" to "Samsung Electronics",
        "F4D7A1" to "Samsung Electronics",
        "F4DDF9" to "Samsung Electronics",
        "F4F15A" to "Apple Inc.",
        "F4F5DB" to "Apple Inc.",
        "F4F951" to "Apple Inc.",
        "F80377" to "Samsung Electronics",
        "F80C93" to "Samsung Electronics",
        "F81D0F" to "Apple Inc.",
        "F82793" to "Apple Inc.",
        "F82D7C" to "Samsung Electronics",
        "F83880" to "Apple Inc.",
        "F84E73" to "Samsung Electronics",
        "F86214" to "Samsung Electronics",
        "F88FCA" to "Apple Inc.",
        "F895C5" to "Samsung Electronics",
        "F8E0BD" to "Apple Inc.",
        "FC183D" to "Samsung Electronics",
        "FC253F" to "Apple Inc.",
        "FC2A54" to "Apple Inc.",
        "FC3497" to "Apple Inc.",
        "FC5125" to "Samsung Electronics",
        "FCA13E" to "Samsung Electronics",
        "FCADB9" to "Apple Inc.",
        "FCC2DE" to "Apple Inc.",
        "FCD848" to "Samsung Electronics",
        "FCDBB3" to "Apple Inc.",
        "FCE997" to "Samsung Electronics"
    )

    fun getManufacturerFromMac(mac: String?): String {
        if (mac == null) return "Unknown"
        val cleanMac = mac.replace(":", "").replace("-", "").trim().uppercase()
        if (cleanMac.length < 6) return "Unknown"
        val oui = cleanMac.substring(0, 6)
        return MAC_OUI_MAP[oui] ?: "Generic / Private"
    }

    /**
     * Checks if Shizuku binder is online and permissions are currently approved.
     */
    fun isShizukuAvailable(context: Context): Boolean {
        if (!Shizuku.pingBinder()) {
            return false
        }
        if (Shizuku.getVersion() < 11) {
            return false
        }
        return Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Queries routing tables on Android via Shizuku shell.
     * Executes 'ip r' and parses the active gateway matching 'ap0' interface.
     */
    fun getHotspotGatewayIp(onResult: (String?) -> Unit) {
        if (Shizuku.getVersion() < 11) {
            Log.e(TAG, "Shizuku API level 11+ is required")
            onResult(null)
            return
        }

        Thread {
            try {
                // Instantiates a privileged shell process via Shizuku using reflection to bypass visibility restrictions
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply { isAccessible = true }
                val process = newProcessMethod.invoke(null, arrayOf("sh"), null, null) as Process
                val os = DataOutputStream(process.outputStream)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errReader = BufferedReader(InputStreamReader(process.errorStream))

                // Requesting routing rules
                val command = "ip r"
                os.writeBytes(command + "\n")
                os.writeBytes("exit\n")
                os.flush()

                val outputLines = ArrayList<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    outputLines.add(line!!)
                }
                process.waitFor()

                os.close()
                reader.close()
                errReader.close()

                // Parse the command output streams for ap0 (tethering interfaces)
                val gatewayIp = parseOutputForAp0(outputLines)

                Handler(Looper.getMainLooper()).post {
                    onResult(gatewayIp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errors invoking Shizuku command execution helper", e)
                Handler(Looper.getMainLooper()).post {
                    onResult(null)
                }
            }
        }.start()
    }

    /**
     * Parse system stdout matches. Equivalent to: grep ap0 | awk '{print $NF}'
     */
    private fun parseOutputForAp0(lines: List<String>): String? {
        for (line in lines) {
            val clean = line.trim()
            // Look for softAP interfaces: "ap0" (standard on modern Snapdragon devices)
            if (clean.contains("ap0") || clean.contains("wlan1") || clean.contains("wlan0")) {
                val parts = clean.split("\\s+".toRegex())
                if (parts.isNotEmpty()) {
                    val srcIndex = parts.indexOf("src")
                    if (srcIndex != -1 && srcIndex + 1 < parts.size) {
                        return parts[srcIndex + 1]
                    }
                    return parts.last() // falls back to trailing awk field
                }
            }
        }
        return null
    }

    /**
     * Executes 'ip neigh' using Shizuku and converts results into a structured list of devices.
     */
    fun getConnectedDevices(onResult: (List<ConnectedDevice>) -> Unit) {
        if (Shizuku.getVersion() < 11) {
            Log.e(TAG, "Shizuku API level 11+ is required for neighbors fetch")
            onResult(emptyList())
            return
        }

        Thread {
            try {
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply { isAccessible = true }
                val process = newProcessMethod.invoke(null, arrayOf("sh"), null, null) as Process
                val os = DataOutputStream(process.outputStream)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errReader = BufferedReader(InputStreamReader(process.errorStream))

                // Execute the neighbors routing inspection excluding stale entries
                val command = "ip neigh | grep -v STALE"
                os.writeBytes(command + "\n")
                os.writeBytes("exit\n")
                os.flush()

                val outputLines = ArrayList<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    outputLines.add(line!!)
                }
                process.waitFor()

                os.close()
                reader.close()
                errReader.close()

                val parsedDevices = parseNeighbors(outputLines)

                Handler(Looper.getMainLooper()).post {
                    onResult(parsedDevices)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errors invoking ip neigh command via Shizuku", e)
                Handler(Looper.getMainLooper()).post {
                    onResult(emptyList())
                }
            }
        }.start()
    }

    /**
     * Executes an arbitrary custom shell command on the device via Shizuku and returns standard output/error stream.
     */
    fun executeCustomCommand(command: String, onResult: (String) -> Unit) {
        if (Shizuku.getVersion() < 11) {
            onResult("Error: Shizuku binder API 11+ required. Ensure Shizuku is authorized and running.")
            return
        }

        Thread {
            try {
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).apply { isAccessible = true }
                val process = newProcessMethod.invoke(null, arrayOf("sh"), null, null) as Process
                val os = DataOutputStream(process.outputStream)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errReader = BufferedReader(InputStreamReader(process.errorStream))

                os.writeBytes(command + "\n")
                os.writeBytes("exit\n")
                os.flush()

                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                
                // Also capture error output
                val errOutput = StringBuilder()
                while (errReader.readLine().also { line = it } != null) {
                    errOutput.append(line).append("\n")
                }

                process.waitFor()

                os.close()
                reader.close()
                errReader.close()

                val result = if (errOutput.isNotEmpty()) {
                    output.toString() + "\nStderr:\n" + errOutput.toString()
                } else {
                    output.toString()
                }

                val finalResult = if (result.trim().isEmpty()) {
                    "Success (No Output / Exit Code: 0)"
                } else {
                    result.trim()
                }

                Handler(Looper.getMainLooper()).post {
                    onResult(finalResult)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errors executing custom command: $command", e)
                Handler(Looper.getMainLooper()).post {
                    onResult("Error running command: ${e.message}\nEnsure Shizuku service is up.")
                }
            }
        }.start()
    }

    private fun parseNeighbors(lines: List<String>): List<ConnectedDevice> {
        val list = ArrayList<ConnectedDevice>()
        for (line in lines) {
            val clean = line.trim()
            if (clean.isEmpty()) continue

            // Example line: "10.154.51.112 dev ap0 lladdr 10:6f:d9:b0:0f:59 REACHABLE"
            val parts = clean.split("\\s+".toRegex())
            if (parts.size >= 4) {
                val ip = parts[0]
                
                val devIndex = parts.indexOf("dev")
                val lladdrIndex = parts.indexOf("lladdr")
                
                val interfaceName = if (devIndex != -1 && devIndex + 1 < parts.size) parts[devIndex + 1] else "unknown"
                val mac = if (lladdrIndex != -1 && lladdrIndex + 1 < parts.size) parts[lladdrIndex + 1] else ""
                val status = parts.last()

                if (mac.isNotEmpty() && 
                    !status.equals("failed", ignoreCase = true) && 
                    !status.equals("incomplete", ignoreCase = true) && 
                    !status.equals("stale", ignoreCase = true)) {
                    val manufacturer = getManufacturerFromMac(mac)
                    list.add(ConnectedDevice(ip, mac, interfaceName, status, manufacturer))
                }
            }
        }
        return list
    }
}
