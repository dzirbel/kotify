import com.dominiczirbel.Secrets
import com.dominiczirbel.network.Spotify

fun main() {
    Secrets.load()
    Secrets["track-id"]?.let { println(Spotify.getTrack(it)) }
}
