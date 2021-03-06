package org.dirtybiologistan.entity;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import javax.inject.Inject;

import org.dirtybiologistan.cryptor.ObjectCryptor;
import org.dirtybiologistan.entity.flag.Flag;
import org.dirtybiologistan.entity.flag.Pixel;
import org.dirtybiologistan.factory.PeopleFactory;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyProperties.Decryption;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


/**
 * @author Jérémy Goutelle for IConvoit Project
 * Edited by Émilien Gallet for the dirtybiologistan.
 */

@Component
public class PeopleDetailsService implements UserDetailsService {

    @Inject
    PeopleFactory peopleList;

    public final PasswordEncoder bCryptPasswordEncoder;

    public PeopleDetailsService() throws NoSuchAlgorithmException{
    	this.bCryptPasswordEncoder =new BCryptPasswordEncoder();
    }
   
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        People people = peopleList.findByUsername(username);
        if (people == null)
            throw new UsernameNotFoundException(username);

        return new User(people.getUsername(), people.getPassword(), people.getRoles());
    }

    public void save(People people,String cipher) {
        encrypt(people,cipher);
        System.out.println("Ecryptage OK "+people.getUsername()+" début de sauvegarde");
        peopleList.save(people);
    }

	public People findByUsername(String username) {
    	People p =peopleList.findByUsername(username);
    	decrypt(p);
        return p;
    }
    

	public List<People> getAllUsers() {
		List<People> pl =(List<People>) peopleList.findAll();
		pl.forEach(e-> decrypt(e));
		return pl;
	}
	/**
	 * On crypte les donnéees dites sensibles afin de se conformer a la CNIL
	 * @param people
	 * @param cipher 
	 * @param pds 
	 */
	private void encrypt(People people, String cipher) {
		people.setLieuIRL(ObjectCryptor.encode(people.getLieuIRL()));
		people.setEmail(ObjectCryptor.encode(people.getEmail()));
		people.setUsername(ObjectCryptor.encode(people.getUsername()));
		people.setName(ObjectCryptor.encode(people.getName()));
		people.setFirstname(ObjectCryptor.encode(people.getFirstname()));
		System.out.println("Encryptage des donneés OK" + people.getUsername()+"encryptage du MDP");
		people.setPassword(bCryptPasswordEncoder.encode(people.getPassword()));
	}
    /**
     * decyptage des données
     * @param p
     */
    private void decrypt(People p) {

    }

	/**
     * Procédure de vérification d'une personne.
     * Notament on vérifie si son pixel n'est pas claim par quelqu'un
     * @param p
     * @return vrai si l'identité est confirmé, faux sinon
     * 
     * L'identité est confirmé si le pixel réclamé n'est pas réquisitionner.
     */
    public boolean checkID(People p,Flag drapeau) {
    	Pixel pix = drapeau.getPixel(p.getLigne(),p.getColone());
    	if (pix==null) {
			return false;
		}
    	p.setPixel(pix);
    	return !pix.getAttribuer();
    }


}
