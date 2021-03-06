////////////////////////////////////////////////////////////////////////
//
//     Copyright (C) 2017 Applied Visions - http://securedecisions.com
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     This material is based on research sponsored by the Department of Homeland
//     Security (DHS) Science and Technology Directorate, Cyber Security Division
//     (DHS S&T/CSD) via contract number HHSP233201600058C.
//
//     Contributor(s):
//              Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.cds.service.protobuf;

import com.denimgroup.threadfix.cds.service.AstamRemoteApplicationService;
import com.denimgroup.threadfix.data.entities.Application;
import com.denimgroup.threadfix.data.entities.astam.AstamApplicationDeployment;
import com.denimgroup.threadfix.mapper.AstamApplicationMapper;
import com.secdec.astam.common.data.models.Appmgmt;
import org.springframework.stereotype.Service;

@Service
public class AstamRemoteApplicationServiceImpl implements AstamRemoteApplicationService {


    private AstamApplicationMapper appMapper;

    private Application application;

    private AstamApplicationDeployment appDeployment;


    public AstamRemoteApplicationServiceImpl( ) {
        appMapper = new AstamApplicationMapper();
    }

    @Override
    public Appmgmt.ApplicationRegistration getAppRegistration(Application application){
        appMapper.setApplication(application);
        return appMapper.getAppRegistration();
    }

    @Override
    public Appmgmt.ApplicationEnvironment getAppEnvironment(){
        //TODO: make sure we fetch ApplicationEnvironment from db not from cache
        //AstamApplicationEnvironment appEnvironment = appDeployment.getApplicationEnvironment();
        //appMapper.setApplicationEnvironment(appEnvironment);
        return appMapper.getAppEnvironment();
    }

    @Override
    public Appmgmt.ApplicationVersion getAppVersion(){
        //TODO: make sure we get refreshed ApplicationVersion since Application has been updated
        //ApplicationVersion appVersion = appDeployment.getApplicationVersion();
        //appMapper.setApplicationVersion(appVersion);
        return appMapper.getAppVersion();
    }

    @Override
    public Appmgmt.ApplicationDeployment getAppDeployment(){
        //TODO: make sure we get refreshed Deployment since Version and Environment has been updated
        //appDeployment =
        //appMapper.setApplicationDeployment(appDeployment, appVersion, appEnvironmnet);
        return appMapper.getAppDeployment();
    }

}
