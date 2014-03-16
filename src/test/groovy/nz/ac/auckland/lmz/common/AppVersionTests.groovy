package nz.ac.auckland.lmz.common

import com.bluetrainsoftware.classpathscanner.ClasspathScanner
import nz.ac.auckland.lmz.flags.Flags
import org.junit.Test

/**
 *
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
class AppVersionTests {
	@Test
	public void ensureVersionIsPickedUp() {
		ClasspathScanner cs = new ClasspathScanner()
		cs.scan(getClass().getClassLoader())

		LmzAppVersion v = new LmzAppVersion()
		v.configured()
		println v.version
		assert v.version
		assert v.version == LmzAppVersion.DEFAULT

		// should not be default, should pick up GAV of this project
		Flags.DEVMODE.turnOn()
		v = new LmzAppVersion()
		v.configured()
		println v.version
		assert v.version
		assert v.version != LmzAppVersion.DEFAULT
		assert !v.version.contains('-SNAPSHOT')
	}
}
