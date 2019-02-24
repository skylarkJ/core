package com.dotcms.auth.providers.jwt.factories;

import static com.dotcms.auth.providers.jwt.JsonWebTokenUtils.CLAIM_UPDATED_AT;
import static com.dotcms.auth.providers.jwt.JsonWebTokenUtils.CLAIM_ALLOWED_NETWORK;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.beans.TokenType;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.control.Try;

import java.io.Serializable;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * This class in is charge of create the Token Factory. It use the
 * "json.web.token.signing.key.factory" on dotmarketing-config.properties (
 * {@link SigningKeyFactory} in order to get a custom implementation
 * 
 * @author jsanca
 * @version 3.7
 * @since June 14, 2016
 */
public class JsonWebTokenFactory implements Serializable {

    /**
     * Used to keep the instance of the JWT Service.
     * Should be volatile to avoid thread-caching
     */
    private volatile JsonWebTokenService jsonWebTokenService = null;
    /**
     * Get the signing key factory implementation from the dotmarketing-config.properties
     */
    public static final String JSON_WEB_TOKEN_SIGNING_KEY_FACTORY =
            "json.web.token.signing.key.factory";
    /**
     * The default Signing Key class
     */
    public static final String DEFAULT_JSON_WEB_TOKEN_SIGNING_KEY_FACTORY_CLASS =
            "com.dotcms.auth.providers.jwt.factories.impl.SecretKeySpecFactoryImpl";

    private JsonWebTokenFactory () {
        // singleton
    }

    private static class SingletonHolder {
        private static final JsonWebTokenFactory INSTANCE = new JsonWebTokenFactory();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static JsonWebTokenFactory getInstance() {

        return JsonWebTokenFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Creates the Json Web Token Service based on the configuration on dotmarketing-config.properties
     * @return JsonWebTokenService
     */
    public JsonWebTokenService getJsonWebTokenService () {

        if (null == this.jsonWebTokenService) {

            synchronized (JsonWebTokenFactory.class) {

                try {

                    if (null == this.jsonWebTokenService) {
                        this.jsonWebTokenService = new JsonWebTokenServiceImpl();
                    }

                } catch (Exception e) {

                    if (Logger.isErrorEnabled(JsonWebTokenService.class)) {
                        Logger.error(JsonWebTokenService.class, e.getMessage(), e);
                    }

                    if (Logger.isDebugEnabled(JsonWebTokenService.class)) {
                        Logger.debug(JsonWebTokenService.class,
                                "There is an error trying to create the Json Web Token Service, going with the default implementation...");
                    }
                }
            }
        }

        return this.jsonWebTokenService;
    } // getJsonWebTokenService

    /**
     * Default implementation
     * @author jsanca
     */
    private class JsonWebTokenServiceImpl implements JsonWebTokenService {

        private Key signingKey;
        private String issuerId;

        private Key getSigningKey() {

            if (null == signingKey) {
                String signingKeyFactoryClass = Config
                        .getStringProperty(JSON_WEB_TOKEN_SIGNING_KEY_FACTORY,
                                DEFAULT_JSON_WEB_TOKEN_SIGNING_KEY_FACTORY_CLASS);

                if (UtilMethods.isSet(signingKeyFactoryClass)) {

                    if (Logger.isDebugEnabled(JsonWebTokenService.class)) {
                        Logger.debug(JsonWebTokenService.class,
                                "Using the signing key factory class: " + signingKeyFactoryClass);
                    }

                    SigningKeyFactory signingKeyFactory =
                            (SigningKeyFactory) ReflectionUtils.newInstance(signingKeyFactoryClass);
                    if (null != signingKeyFactory) {

                        signingKey = signingKeyFactory.getKey();
                    }
                }
            }

            return signingKey;
        }

        private String getIssuer() {

            if (null == issuerId) {
                issuerId = ClusterFactory.getClusterId();
            }

            return issuerId;
        }

        
        @Override
        public String generateToken(final ApiToken apiToken) {
            Map<String,Object> claims = Try.of(()->new ObjectMapper().readValue(apiToken.claims, HashMap.class)).getOrElse(new HashMap<>());
            
            claims.put(CLAIM_UPDATED_AT, apiToken.modDate);
            claims.put(CLAIM_ALLOWED_NETWORK, apiToken.allowFromNetwork);
            
            //Let's set the JWT Claims
            final JwtBuilder builder = Jwts.builder()
                    .setId(UUID.randomUUID().toString())
                    .setClaims(claims)
                    .setSubject(apiToken.id)
                    .setExpiration(apiToken.expires)
                    .setIssuedAt(apiToken.issueDate)
                    .setIssuer(ClusterFactory.getClusterId())
                    .setNotBefore(apiToken.issueDate);
                    
            
            
            return generateToken(builder);
            
        }
        
        
        
        
        
        
        

        private String generateToken(final JwtBuilder jwtBuilder) {
            //The JWT signature algorithm we will be using to sign the token
            jwtBuilder.signWith(SignatureAlgorithm.HS256, this.getSigningKey());
            jwtBuilder.setIssuer(this.getIssuer());
            jwtBuilder.setIssuedAt(new Date());
            return jwtBuilder.compact();
        }
        
        @Override
        public String generateToken(final JWTBean jwtBean) {

            //Let's set the JWT Claims
            final JwtBuilder builder = Jwts.builder()
                    .setId(jwtBean.getId())
                    .claim(CLAIM_UPDATED_AT, jwtBean.getModificationDate())
                    .claim(CLAIM_ALLOWED_NETWORK, jwtBean.getClaims().get(CLAIM_ALLOWED_NETWORK))
                    .setSubject(jwtBean.getSubject())
                    .setExpiration(jwtBean.getExpiresDate());

            

            //Builds the JWT and serializes it to a compact, URL-safe string
            return generateToken(builder);
        } // generateToken.

        @Override
        public JWToken parseToken(final String jsonWebToken) {

            JWToken jwtToken = null;

            if ( !UtilMethods.isSet(jsonWebToken) ) {

                throw new IllegalArgumentException("Security Token not found");
            }

            try {
                //This line will throw an exception if it is not a signed JWS (as expected)
                final Jws<Claims> jws = Jwts.parser()
                        .setSigningKey(this.getSigningKey())
                        .parseClaimsJws(jsonWebToken);

                if (null != jws) {

                    final Claims body = jws.getBody();
                    if (null != body) {

                        // Validate the issuer is correct, meaning the same cluster id
                        if (!this.getIssuer().equals(body.getIssuer())) {
                            IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), jws.getBody(), "Invalid issuer");
                            claimException.setClaimName(Claims.ISSUER);
                            claimException.setClaimValue(body.getIssuer());
                            throw claimException;
                        }

                        
                        jwtToken = TokenType.getTokenType(body.getSubject()) == TokenType.USER_TOKEN ? 
                                new JWTBean(body.getId(),
                                        body.getSubject(),
                                        body.getIssuer(),
                                        body.get(CLAIM_UPDATED_AT, Date.class),
                                        (null != body.getExpiration()) ? body.getExpiration().getTime() : 0, 
                                        body
                                        )
                               : ApiToken.builder().withClaims(body)
                                       .withClusterId(body.getIssuer())
                                       .withExpires(body.getExpiration())
                                       .withAllowFromNetwork((String) body.getOrDefault(CLAIM_ALLOWED_NETWORK, null))
                                       .withIssueDate(body.getIssuedAt())
                                       .withId(body.getId())
                                       .withModDate((Date)body.get(CLAIM_UPDATED_AT, Date.class))
                                       .build();
                                       
                        
                        
                        if(jwtToken.getTokenType() == TokenType.API_TOKEN) {
                            Optional<ApiToken> apiTokenOpt = APILocator.getApiTokenAPI().findApiToken(jwtToken.getSubject());
                            if(!apiTokenOpt.isPresent()) {
                                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), jws.getBody(), "Invalid API Token");
                                claimException.setClaimName(Claims.SUBJECT);
                                claimException.setClaimValue(body.getSubject());
                                throw claimException;
                            }
                            ApiToken apiToken = apiTokenOpt.get();
                            if(apiToken.isExpired()) {
                                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), jws.getBody(), "API Token Expired");
                                claimException.setClaimName(Claims.EXPIRATION);
                                claimException.setClaimValue(apiToken.expires);
                                throw claimException;
                                
                            }
                            if(apiToken.isRevoked()) {
                                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), jws.getBody(), "API Token Revoked");
                                claimException.setClaimName(Claims.EXPIRATION);
                                claimException.setClaimValue(apiToken.revoked);
                                throw claimException;
                                
                            }
                            if(apiToken.isNotBeforeDate()) {
                                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), jws.getBody(), "API Token Not Before");
                                claimException.setClaimName(Claims.NOT_BEFORE);
                                claimException.setClaimValue(apiToken.issueDate);
                                throw claimException;
                                
                            }
                            if(!apiToken.isValid()) {
                                IncorrectClaimException claimException = new IncorrectClaimException( jws.getHeader(), jws.getBody(), "Invalid API Token");
                                claimException.setClaimName(Claims.SUBJECT);
                                claimException.setClaimValue(body.getSubject());
                                throw claimException;
                            }
                        }
                        
                    }
                }
            } catch (ExpiredJwtException e) {

                if (Logger.isDebugEnabled(this.getClass())) {
                    Logger.debug(this.getClass(), e.getMessage(), e);
                }

                jwtToken = null;
            }

            return jwtToken;
        } // parseToken.
        

        
    } // JsonWebTokenServiceImpl.

} // E:O:F:JsonWebTokenFactory.
