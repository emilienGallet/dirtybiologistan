/**
 * Code of Émilien Gallet
 * next step improuve teh data import to be more eco-friendly
 */
function displayFlag(boolean) {
	let x = document.getElementById('leDrapeau').style.display
	if (x == "none") {
		console.log("afficher drapeau")
		document.getElementById('displayFlag').value = "Chargement du drapeau"
		try {
			document.getElementById('pixelisator').style.display = "none"
			document.getElementById('lesVoisins').style.display = "none"
		} catch (e) {

		}
		setTimeout(realDisplayFlag, 1000)

	} else {
		console.log("cacher drapeau")
		try {
			document.getElementById('pixelisator').style.display = "inline-block"
			document.getElementById('lesVoisins').style.display = "inline-block"
		} catch (e) {

		}
		document.getElementById('leDrapeau').style.display = "none"
		document.getElementById('displayFlag').value = "Afficher le drapeau"
	}
}
function realDisplayFlag() {
	document.getElementById('leDrapeau').style.display = "inherit"
	try{
		document.getElementById('pixelisator').style.display
		document.getElementById('displayFlag').value = "Afficher le Pixelisator"
	}catch(e){
		document.getElementById('displayFlag').value = "Cacher le Drapeau"
	}
	
	
}