package com.appfamilia.feature.auth;

import android.app.Activity;
import android.os.CancellationSignal;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.appfamilia.BuildConfig;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/** Abre o seletor de contas do Google e devolve o ID token, que o backend confere. */
public final class GoogleLogin {

    public interface Retorno {
        void token(String idToken);

        void erro(String mensagem);
    }

    private GoogleLogin() {
    }

    /** O botão do Google só aparece quando o client-id foi configurado em gradle.properties. */
    public static boolean disponivel() {
        return !BuildConfig.GOOGLE_CLIENT_ID.isEmpty();
    }

    public static void pedir(Activity activity, Retorno retorno) {
        GetGoogleIdOption opcao = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                .build();
        GetCredentialRequest pedido = new GetCredentialRequest.Builder()
                .addCredentialOption(opcao)
                .build();

        CredentialManager.create(activity).getCredentialAsync(
                activity,
                pedido,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse resposta) {
                        Credential credencial = resposta.getCredential();
                        if (credencial instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credencial.getType())) {
                            retorno.token(GoogleIdTokenCredential.createFrom(credencial.getData()).getIdToken());
                        } else {
                            retorno.erro("Não foi possível obter a conta Google");
                        }
                    }

                    @Override
                    public void onError(GetCredentialException falha) {
                        retorno.erro("Login com Google cancelado ou indisponível");
                    }
                });
    }
}
