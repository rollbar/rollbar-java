package com.rollbar.web.provider;

import com.rollbar.api.payload.data.Person;
import com.rollbar.notifier.provider.Provider;
import com.rollbar.web.listener.RollbarRequestListener;
import javax.servlet.http.HttpServletRequest;

public class PersonProvider implements Provider<Person> {

  @Override
  public Person provide() {
    HttpServletRequest request = RollbarRequestListener.getServletRequest();

    if(request != null && request.getUserPrincipal() != null) {
      return new Person.Builder()
          .id(request.getUserPrincipal().getName())
          .build();
    }
    return null;
  }
}
