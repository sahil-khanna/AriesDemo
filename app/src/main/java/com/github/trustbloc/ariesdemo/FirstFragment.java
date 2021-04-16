/*
 * Copyright SecureKey Technologies Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.github.trustbloc.ariesdemo;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import org.hyperledger.aries.api.AriesController;
import org.hyperledger.aries.api.DIDExchangeController;
import org.hyperledger.aries.api.Handler;
import org.hyperledger.aries.api.VerifiableController;
import org.hyperledger.aries.ariesagent.Ariesagent;
import org.hyperledger.aries.config.Options;
import org.hyperledger.aries.models.RequestEnvelope;
import org.hyperledger.aries.models.ResponseEnvelope;

import java.nio.charset.StandardCharsets;

class MyHandler implements Handler {

    String lastTopic, lastMessage;

    public String getLastNotification() {
        return lastTopic + "\n" + lastMessage;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("LongLogTag")
    @Override
    public void handle(String topic, byte[] message) {
        lastTopic = topic;
        lastMessage = new String(message, StandardCharsets.UTF_8);

        Log.d("HANDLERSTATE", lastTopic);
        Log.d("HANDLERSTATE", lastMessage);
    }
}

public class FirstFragment extends Fragment implements View.OnClickListener {


    Button btnCreateInvitation, btnAcceptInvitation, btnGetConnections, btnAcceptExchangeRequested;
    EditText edtJsonData;
    //http://10.233.48.97/
    String url = "", websocketURL = "", retrievedCredentials = "";

    final String agentName = "Bob";

    //ReqData is a json from edittext that will be sent to request
    String reqData = "";
    /*String reqData = "{\n\t\t\"serviceEndpoint\":\"http://10.233.48.95:9101\",\n\t\t\"" +
            "recipientKeys\":[\"FDmegH8upiNquathbHZiGBZKwcudNfNWPeGQFBt8eNNi\"],\n\t\t\"" +
            "@id\":\"a35c0ac6-4fc3-46af-a072-c1036d036057\",\n\t\t\"label\":\"agent\",\n\t\t\"" +
            "@type\":\"https://didcomm.org/didexchange/1.0/invitation\"}";
*/

    boolean useLocalAgent = true;

    AriesController agent;

    MyHandler handler;

    @SuppressLint("LongLogTag")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setAgent() {
        Options opts = new Options();
        opts.setAgentURL(url);
        opts.setLabel(agentName);
        opts.setAutoAccept(true);
        setTitle();
        opts.setUseLocalAgent(true);
      //  opts.setWebsocketURL(websocketURL);

        // create an aries agent instance
        try {
            agent = Ariesagent.new_(opts);
            handler = new MyHandler();
            final String registrationID = agent.registerHandler(handler, "didexchange_states");
            reqData = "{\t\t\n\"@id\":\""+registrationID+"\",\n\t\t\"label\":\"" + agentName + "\",\n\t\t\"@type\":\"https://didcomm.org/didexchange/1.0/invitation\"\n}";
            edtJsonData.setText(reqData);
            Log.v("RegistratioonId",registrationID);
            Toast.makeText(getContext(),agentName+" created",Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(),agentName+" was not created",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setTitle() {
        MainActivity act = (MainActivity) getActivity();
        if (act != null) {
            act.setTitl(agentName);
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void getCredentials() {
        if (!useLocalAgent && url.equals("")) {
            TextView credentials = requireView().findViewById(R.id.credentials);
            credentials.setText("A remote agent URL must be provided");
        } else {
            ResponseEnvelope res = new ResponseEnvelope();
            try {

                // create a controller
                VerifiableController v = agent.getVerifiableController();

                // perform an operation
                byte[] data = "{}".getBytes(StandardCharsets.UTF_8);
                res = v.getCredentials(new RequestEnvelope(data));
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (res.getError() != null) {
                Toast.makeText(getContext(), "You need create an agent", Toast.LENGTH_LONG).show();

                if (!res.getError().getMessage().equals("")) {
                    System.out.println(res.getError().getMessage());
                }
            }

            retrievedCredentials = new String(res.getPayload(), StandardCharsets.UTF_8);
        }
    }

    @SuppressLint({"SetTextI18n", "LongLogTag"})
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void receiveInvitation() {
        if (!useLocalAgent && url.equals("") && websocketURL.equals("")) {
            TextView text = requireView().findViewById(R.id.notification_result);
            text.setText("An agent URL and websocket URL must be provided for remote agents");
        } else {
            ResponseEnvelope res = new ResponseEnvelope();
            try {

                // call did exchange method
                byte[] data = reqData.getBytes(StandardCharsets.UTF_8);

                RequestEnvelope requestEnvelope = new RequestEnvelope(data);
                DIDExchangeController didex = agent.getDIDExchangeController();
                res = didex.receiveInvitation(requestEnvelope);
                if (res.getError() != null && !res.getError().getMessage().isEmpty()) {
                    String error = res.getError().getMessage();
                    setError(error);
                    Log.d("failed to receive invitation: ", error);
                } else {
                    clearError();
                    String receiveInvitationResponse = new String(res.getPayload(), StandardCharsets.UTF_8);
                    edtJsonData.setText(receiveInvitationResponse);
                    Log.d("received invitation with: ", receiveInvitationResponse);
                }

            } catch (Exception e) {
                Toast.makeText(getContext(), "You need create an agent", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            if (res.getError() != null) {
                if (!res.getError().getMessage().equals("")) {
                    System.out.println(res.getError().getMessage());
                }
            }

        }
    }


    @SuppressLint("LongLogTag")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void acceptExchangeRequest() {
        if (!useLocalAgent && url.equals("") && websocketURL.equals("")) {
            TextView text = requireView().findViewById(R.id.notification_result);
            text.setText("An agent URL and websocket URL must be provided for remote agents");
        } else {
            ResponseEnvelope res = new ResponseEnvelope();
            try {

                // call did exchange method
                byte[] data = reqData.getBytes(StandardCharsets.UTF_8);

                RequestEnvelope requestEnvelope = new RequestEnvelope(data);
                DIDExchangeController didex = agent.getDIDExchangeController();
                res = didex.acceptExchangeRequest(requestEnvelope);
                if (res.getError() != null && !res.getError().getMessage().isEmpty()) {
                    String error = res.getError().getMessage();
                    setError(error);
                    Log.d("failedaccept:", error);
                } else {
                    clearError();
                    String receiveInvitationResponse = new String(res.getPayload(), StandardCharsets.UTF_8);
                    edtJsonData.setText(receiveInvitationResponse);
                    Log.d("accepted ", receiveInvitationResponse);
                }

            } catch (Exception e) {
                Toast.makeText(getContext(), "Crie um agente", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            if (res.getError() != null) {
                if (!res.getError().getMessage().equals("")) {
                    System.out.println(res.getError().getMessage());
                }
            }

        }
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        btnCreateInvitation = view.findViewById(R.id.btn_create_invitation);
        btnAcceptInvitation = view.findViewById(R.id.btn_accept_invitation);
        btnAcceptExchangeRequested = view.findViewById(R.id.btn_accept_exchange_requested);
        btnGetConnections = view.findViewById(R.id.btn_get_connections);
        edtJsonData = view.findViewById(R.id.didex_receiveInvitation_req);
        btnGetConnections.setOnClickListener(this);
        btnCreateInvitation.setOnClickListener(this);
        btnAcceptInvitation.setOnClickListener(this);
        btnAcceptExchangeRequested.setOnClickListener(this);
        return view;
    }

    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final EditText urlInput = view.findViewById(R.id.agent_url);
        urlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0)
                    url = s.toString();
            }
        });

        final EditText websocketURLInput = view.findViewById(R.id.websocket_url);
        websocketURLInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0)
                    websocketURL = s.toString();
            }
        });

        final EditText receiveInvitationInput = view.findViewById(R.id.didex_receiveInvitation_req);
        receiveInvitationInput.setText(reqData);
        receiveInvitationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0)
                    reqData = s.toString();
            }
        });

        view.findViewById(R.id.use_local_agent).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                useLocalAgent = !useLocalAgent;

                int bool = useLocalAgent ? View.INVISIBLE : View.VISIBLE;
                urlInput.setVisibility(bool);

                CheckBox btn = requireView().findViewById(R.id.use_local_agent);
                btn.setChecked(useLocalAgent);

                TextView credentials = requireView().findViewById(R.id.credentials);
                credentials.setText("");

                Button getCredsBtn = (Button) requireView().findViewById(R.id.button_get_credentials);
                getCredsBtn.setEnabled(false);

                TextView notifs = requireView().findViewById(R.id.notification_result);
                notifs.setText("");
            }
        });

        view.findViewById(R.id.button_newAgent).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                setAgent();

                Button getCredsBtn = (Button) requireView().findViewById(R.id.button_get_credentials);
                getCredsBtn.setEnabled(true);

                Button rcvInvitationBtn = (Button) requireView().findViewById(R.id.button_receiveInvitation);
                rcvInvitationBtn.setEnabled(true);
            }
        });

        Button rcvInvitationBtn = view.findViewById(R.id.button_receiveInvitation);


        view.findViewById(R.id.button_get_credentials).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                getCredentials();
                TextView credentials = requireView().findViewById(R.id.credentials);
                credentials.setText(retrievedCredentials);
            }
        });

        view.findViewById(R.id.button_receiveInvitation).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                receiveInvitation();
                TextView notifs = requireView().findViewById(R.id.notification_result);
                notifs.setText(handler.getLastNotification());
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_create_invitation) {
            createInvitation();
        } else if (v.getId() == R.id.btn_accept_invitation) {
            acceptInvitation();
        } else if (v.getId() == R.id.btn_get_connections) {
            getConnections();
        } else if (v.getId() == R.id.btn_accept_exchange_requested) {
            acceptExchangeRequest();
        }
    }

    private void getConnections() {
        ResponseEnvelope res = new ResponseEnvelope();
        try {

            RequestEnvelope requestEnvelope = new RequestEnvelope("{\"state\":\"requested\"}".getBytes(StandardCharsets.UTF_8));
            DIDExchangeController didex = agent.getDIDExchangeController();
            res = didex.queryConnections(requestEnvelope);
            TextView txtConnections = getView().findViewById(R.id.txt_connections);
            if (res.getError() != null && !res.getError().getMessage().isEmpty()) {
                String error = res.getError().getMessage();
                setError(error);
                txtConnections.setText(error);
            } else {
                clearError();
                String connections = new String(res.getPayload(), StandardCharsets.UTF_8);
                txtConnections.setText(connections);
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Crie um agente", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        if (res.getError() != null) {


            if (!res.getError().getMessage().equals("")) {
                System.out.println(res.getError().getMessage());
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void createInvitation() {
        ResponseEnvelope res = new ResponseEnvelope();
        try {

            // call did exchange method
            byte[] data = reqData.getBytes(StandardCharsets.UTF_8);

            RequestEnvelope requestEnvelope = new RequestEnvelope(data);
            DIDExchangeController didex = agent.getDIDExchangeController();
            res = didex.createInvitation(requestEnvelope);
            if (res.getError() != null && !res.getError().getMessage().isEmpty()) {
                String error = res.getError().getMessage();
                setError(error);
                Log.d("CREATEINVITA", error);
            } else {
                clearError();
                String createInvitationResponse = new String(res.getPayload(), StandardCharsets.UTF_8);
                edtJsonData.setText(createInvitationResponse);
                Log.d("CREATEINVITA", createInvitationResponse);
                Toast.makeText(getContext(), createInvitationResponse, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Crie um agente", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        if (res.getError() != null) {

            if (!res.getError().getMessage().equals("")) {
                System.out.println(res.getError().getMessage());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void acceptInvitation() {

        RequestEnvelope requestEnvelope = new RequestEnvelope(reqData.getBytes(StandardCharsets.UTF_8));

        try {
            DIDExchangeController didex = agent.getDIDExchangeController();
            ResponseEnvelope res = didex.acceptInvitation(requestEnvelope);
            if (res.getError() != null) {
                String error = res.getError().getMessage();
                setError(error);
                Log.v("ERROR", "aceitando convite error " + error);
            } else {
                clearError();
                String acceptedInvitation = new String(res.getPayload(), StandardCharsets.UTF_8);
                edtJsonData.setText(acceptedInvitation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setError(String error) {
        final TextView txtError = getView().findViewById(R.id.txt_error);
        txtError.setText(error);
    }

    private void clearError() {
        final TextView txtError = getView().findViewById(R.id.txt_error);
        txtError.setText("");
    }
}
