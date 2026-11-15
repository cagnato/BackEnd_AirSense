package br.pucpr.airsense.common

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

/**
 * Tira os espaços das pontas já na leitura do JSON, antes da validação.
 * Assim " ana@x.com " passa no @Email, como no app.
 */
class TrimmedStringDeserializer : StdDeserializer<String>(String::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String =
        p.valueAsString.trim()
}
