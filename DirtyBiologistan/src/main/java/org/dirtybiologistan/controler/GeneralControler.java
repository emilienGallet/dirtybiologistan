package org.dirtybiologistan.controler;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.dirtybiologistan.DeployInit;
import org.dirtybiologistan.entity.People;
import org.dirtybiologistan.entity.PeopleDetailsService;
import org.dirtybiologistan.entity.PeopleRole;
import org.dirtybiologistan.entity.PeopleValidator;
import org.dirtybiologistan.entity.flag.Flag;
import org.dirtybiologistan.entity.flag.Pixel;
import org.dirtybiologistan.repository.AssociationRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @version 1.0
 * @author emilien Goal : Website of the newest country : DirtyBiologistan
 * A spliter en plusieurs controlleur en V2
 */
@Controller
public class GeneralControler {

	@Inject
	PeopleDetailsService pds;

	@Inject
	PeopleValidator peopleValidator;

	@Inject
	AssociationRepository assotiations;

	private Flag drapeau = new Flag();
	
	/**
	 * Permet de déchiffrer ce qui est stocker en BDD a partir d'une clé généré par l'utilisateur et par le système
	 */
	private HashMap<String, String> decipher;
	private String cipher;
	private Boolean start;

	/**
	 * Constructeur permetant au démarage l'import des données issue de Codati.
	 */
	public GeneralControler(){
		start=true;
		cipher = System.getProperties().getProperty("user.dir");
		System.out.println("[CIPHER] = "+cipher);
		decipher = new HashMap<>();
	}

	/**
	 * 
	 * @param m le modèle
	 * @return l'index du site web en thymeleaf
	 */
	@GetMapping("/")
	public String home(Model m) {		
		m.addAttribute("ressourceesDeploy", DeployInit.PathResourcesDeploy2);
		m.addAttribute("isConnected", getCurentUserOrNull());
		return "index";
	}

	/**
	 * Retourne la constitution de la micronation
	 * 
	 * @return constitution.html
	 */
	@GetMapping("/constitution")
	public String consitution(Model m) {
		m.addAttribute("ressourceesDeploy", DeployInit.PathResourcesDeploy2);
		m.addAttribute("isConnected", getCurentUserOrNull());
		return "constitution";
	}

	/**
	 * 
	 * @return citoyen.html
	 */
	@GetMapping("/citoyens")
	public String citoyens(Model m) {
		People p;
		if ((p = getCurentUserOrNull())==null) {
			return "redirect:/";
		}
		return "redirect:/idCard/"+Sha512DigestUtils.shaHex(p.getId().toString());
	}

	/**
	 * Le mettre en rest
	 * 
	 * @return le tag de la personne
	 */
	@GetMapping("/citoyens/{colone}/{ligne}")
	public String citoyensID(Model m) {
		m.addAttribute("ressourceesDeploy", DeployInit.PathResourcesDeploy2);
		m.addAttribute("isConnected", getCurentUserOrNull());
		return "citoyens";
	}

	/**
	 * 
	 * @param m
	 * @return drapeau.html avec les param passer a thymeleaf.
	 * @throws Exception
	 */
	@GetMapping("/drapeau")
	public String flag(Model m) throws Exception {
		/**
		 * La localisation des resources est différente lors du déploiment.
		 */
		m.addAttribute("pixies", drapeau.drapeau);
		m.addAttribute("ressourceesDeploy", DeployInit.PathResourcesDeploy2);

		//Si l'utilisateur est connecter, alors il peux afficher ces pixels voisins et parametrer son pixel.
		try {
			People p = getCurentUser();
			if (p.estIlCitoyen()) {
				m.addAttribute("voisins", p.getVoisin(drapeau.drapeau,6));
				m.addAttribute("isConnected", p);
			}else {
				m.addAttribute("voisins", null);
				m.addAttribute("isConnected", null);
			}
		} catch (Exception e) {
		//Sinon on n'affiche rien
			m.addAttribute("voisins",null);
			m.addAttribute("isConnected", null);
		}

		return "realFlag/flag";
	}
	/**
	 * requete de pixelColor.js
	 * @param couleur
	 * @return en JSON la réponse oui ou non si le pixel a été bien modifier
	 */
	@PostMapping("/drapeau")
	@ResponseBody
	public String modifierPixel(@RequestBody String couleur) {
		try {
			couleur = couleur.replace("\"", "");
			System.out.println(couleur);
			People p = getCurentUser();
			if (p==null) {
				return "{\"result\":\"no\"}";
			}
			p.setPixel(this.drapeau,couleur);//
			System.out.println("ok");
			return "{\"result\":\""+p.getColone()+":"+p.getLigne()+"|"+couleur+"\"}";
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("NON");
			return "{\"result\":\"no\"}";
		}
	}

	/**
	 * 
	 * @return error.html
	 */
	public String error(Model m) {
		m.addAttribute("isConnected", getCurentUserOrNull());
		return "error";
	}

	/**
	 * Affecte un pixel au citoyen
	 * 
	 * @throws Exception
	 */
	private Pixel affecterPixel() throws Exception {
		return drapeau.rajouterNewPixel("#0000");//pixel TRRANSPARENT
	}
	
	/**
	 * Retour la page de connection
	 * 
	 * @return login.html
	 */
	@GetMapping("/login")
	public String loginpage(Model m) {
		if (getCurentUserOrNull()!=null) {
			return "redirect:/";
		}
		m.addAttribute("ressourceesDeploy", DeployInit.PathResourcesDeploy2);
		m.addAttribute("p", new People());
		m.addAttribute("isConnected", getCurentUserOrNull());
		return "login";
	}
	
	/**
	 * Retour la page de connection
	 * 
	 * @return login.html
	 */
	@PostMapping("/login")
	public String login(@ModelAttribute("p") People p, BindingResult bindingResult) {
		//EMPECHER LE BRUT FORCE (limit par IP) 
		
		if (getCurentUserOrNull()!=null) {
			return "redirect:/";
		}
		
		try {
			if (!isCorrect(p)) {
				return "redirect:/login";
			}
		}catch (Exception e) {
			return "redirect:/login";//TODO indiquer plus tard la source de l'erreur
		}
		//Cree une méthode pour /register aussi
		addMapDecipher(p.getUsername(),p.getPassword());
		
		//Partie a voir.
		UserDetails userDetails = pds.loadUserByUsername(p.getUsername());
		UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
				userDetails, p.getPassword(), userDetails.getAuthorities());

		if (usernamePasswordAuthenticationToken.isAuthenticated()) {
			SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
		}
		return "redirect:/";
	}

	private boolean isCorrect(People p) {
		return pds.bCryptPasswordEncoder.matches(p.getPassword(), pds.findByUsername(p.getUsername()).getPassword());	
	}

	private String addMapDecipher(String username,String password) {
		String wholeCipher = Sha512DigestUtils.shaHex(password+this.cipher);
		decipher.put(username,wholeCipher);
		return wholeCipher;
	}

	/**
	 * 
	 * @param m
	 * @return register.html
	 */
	@GetMapping("/register")
	public String register(Model m) {
		if (getCurentUserOrNull()!=null) {
			return "redirect:/";
		}
		m.addAttribute("register", new People());
		m.addAttribute("roles", PeopleRole.values());
		m.addAttribute("ressourceesDeploy", DeployInit.PathResourcesDeploy2);
		m.addAttribute("isConnected", null);
		return "register";
	}

	/**
	 * 
	 * @param p
	 * @param bindingResult
	 * @return la route pour afficher la carte d'identité 
	 * sinon renvoie sur le formulaire ou la route de la police (non définie)
	 */
	@PostMapping("/register")
	public String addUser(@ModelAttribute("register") People p, BindingResult bindingResult) {
		peopleValidator.validate(p, bindingResult);
		if (bindingResult.hasErrors()) {
			return "/register";
		}
		if (p.defineRoleGranted()) {
			if (p.getRoles().contains(PeopleRole.NEW_CITOYEN)) {
				p.getRoles().remove(PeopleRole.NEW_CITOYEN);
				try {
					p.setPixel(this.affecterPixel());
				} catch (Exception e) {
					// TODO rajouter les attribut pour la page d'erreur 
					e.printStackTrace();
					return "error";
				}
				p.getRoles().add(PeopleRole.CITOYEN);
			}else if(p.getRoles().contains(PeopleRole.CITOYEN)){
				// TODO
				// Mettre un mdp pour pouvoir utiliser ce pixel
				// procédure de vérification manuel par silicyium
				// En attendant ... 
				if(!pds.checkID(p,drapeau)) {
					return "redirect:/police";//renvoyer sur une convocation au comissariat x)					
				}
			}
			// Tout est ok on va enregistrer la personne et l'auto connecter.
			System.out.println("Tout est OK pour" +p.getUsername()+" Ajout de la clé de chiffrement");
			String chi =addMapDecipher(p.getUsername(),p.getPassword());
			System.out.println("Clé de chiffrement pour "+p.getUsername()+ "OK");
			pds.save(p,chi);
			System.out.println("Sauvegarde pour "+p.getUsername()+ "OK");
			//Décryptage des données présente en BDD exposé au public comme le veux une spécification dite en assemblée
			try {
				UserDetails userDetails = pds.loadUserByUsername(p.getUsername());
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
						userDetails, p.getPassword(), userDetails.getAuthorities());

				if (usernamePasswordAuthenticationToken.isAuthenticated()) {
					SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
					System.err.println("OK");// log.debug(String.format("Auto login %s successfully!", email));
				}
			} catch (Exception e) {
				//Partie non atteinte normalement
				System.err.println("NON OK");// log.error(e.getMessage(), e);
			}
			return "redirect:/idCard/" + Sha512DigestUtils.shaHex(p.getId().toString());
		}
		return "/register";

	}
	
	/**
	 * 
	 * @return la personne conecter ou renvoie la valeu null
	 */
	private People getCurentUserOrNull() {
		try {
			return getCurentUser();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

	private People getCurentUser() throws Exception {
		UserDetails userD = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return pds.findByUsername(userD.getUsername());
	}

	/**
	 * 
	 * @param idCard
	 * @param m
	 * @return carteIdentiter.html
	 * sinon la route de la police (non définie)
	 */
	@GetMapping("/idCard/{idCard}")
	public String carteIdentite(@PathVariable String idCard, Model m) {
		try {
			People p = getCurentUser();
			if (Sha512DigestUtils.shaHex(p.getId().toString()).contentEquals(idCard)) {
				String asked = Sha512DigestUtils.shaHex(idCard+cipher);
				m.addAttribute("people", p);
				m.addAttribute("cardIdentification", asked);
				m.addAttribute("ressourceesDeploy", DeployInit.PathResourcesDeploy2);
				m.addAttribute("isConnected", p);
				return "carteIdentiter";// html a crée
			} else {
				return "redirect:/police";
			}
		} catch (Exception e) {
			return "redirect:/police";
		}

	}

	@GetMapping("/assotiation")
	public String assosList(Model m) {
		m.addAttribute("assotiations", assotiations);
		m.addAttribute("isConnected", getCurentUserOrNull());
		return "register";
	}
	
	@GetMapping("/police")
	public String police(Model m) {
		m.addAttribute("ressourceesDeploy", DeployInit.PathResourcesDeploy2);
		m.addAttribute("assotiations", assotiations);
		m.addAttribute("isConnected", getCurentUserOrNull());
		return "police";
	}
	
	/**
	 * Renvoie toute les données de la base de donnée afin de rendre acte des données personnel que l'on récolte.
	 * @return
	 * @throws IOException 
	 */
    @RequestMapping("/allDB")
    @ResponseBody
    public List<Object> allDB(HttpServletResponse reponse) throws IOException{
    	if (getCurentUserOrNull()==null) {
			reponse.sendRedirect(DeployInit.PathResourcesDeploy2);
		}
    	List<Object> data = new ArrayList<Object>();
    	data.add(pds.getAllUsers());
    	data.add(this.drapeau);
    	return data;
    }
    
    /**
	 * Renvoie toute les données de la base de donnée à propos du drapeau
	 * @return the flag
	 * ATTENTION : ceci est destiner a évolution dans les prochaine version pour prendre en compte un clé d'API.
	 */
    @RequestMapping("/allFlagDB")
    @ResponseBody
    public List<Object> allFlagDB(){
    	List<Object> data = new ArrayList<Object>();
    	//On pourra rajouter des info nécéssaire si besoin comme par exemple le nombre de pixel
    	data.add(this.drapeau);
    	return data;
    }
    
    @GetMapping("/loadDataFromCodati")
    @ResponseBody
    public synchronized String loadDataFromCodati() {
		/////////////////////////////////////////////////////////////////////////////////////////////
		//A remplacer apres le 1er start par une version sauvegarder du drapeau
		if (start) {
			try {
				drapeau.chargerDataFromFouloscopieAndCodati();
			} catch (Exception e) {
				//TODO Auto-generated catch block
				e.printStackTrace();
				return "{\"result\":\""+e.getMessage()+"\"}";
			}
		}
		/////////////////////////////////////////////////////////////////////////////////////////////
		start=false;
    	return "{\"result\":\"loaded\"}";
    }
}