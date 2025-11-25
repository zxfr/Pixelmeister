#include <UTFT.h>
#include <uText.h>

#define null NULL

UTFT utft(ITDB24,38,39,40,41);
uText utext(&utft, $[width], $[height]);

$[declarations]

$[functions]

	void setup() {
		utft.InitLCD($[orientation]);
$[sketchcode_s]
	}

	void loop() {
$[sketchcode_l]
	}

$[resources]
