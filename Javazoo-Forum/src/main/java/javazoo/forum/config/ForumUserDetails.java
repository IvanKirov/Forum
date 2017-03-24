package javazoo.forum.config;

/**
 * Created by danie on 3/24/2017.
 */

import antlr.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import javazoo.forum.entity.User;

import java.util.ArrayList;
import java.util.Collection;

public class ForumUserDetails extends User implements UserDetails{
    private ArrayList<String>roles;
    private User user;

    public ForumUserDetails(User user, ArrayList<String> roles){
        super(user.getEmail(), user.getFullName(), user.getPassword());

        this.roles = roles;
        this.user = user;
    }
    public User getUser(){
        return this.user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        String userRoles = StringUtils.collectionToCommaDelimitedString(this.roles);
        return AuthorityUtils.commaSeparatedStringToAuthorityList(userRoles);
    }

    @Override
    public String getUsername() {
        return super.getEmail();
    }


    @Override
    public boolean isAccountNonExpired(){
        return true;
    }
    @Override
    public boolean isAccountNonLocked(){
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }
    @Override
    public boolean isEnabled(){
        return true;
    }


}
