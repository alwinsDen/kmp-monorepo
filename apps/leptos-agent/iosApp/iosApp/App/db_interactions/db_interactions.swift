import Shared

func saveModelPrefs(
    userId: String,
    modelProvider: String,
    model: String,
    effort: String,
    useOnlineLeptos: Bool
){
    let db  = DatabaseProvider().database
    let dao = db.getDao()
    let prefs = ModelPreferencesEntity(
        userId: userId,
        modelProvider: modelProvider,
        model: model,
        effortLevel: effort,
        useOnlineLeptos: useOnlineLeptos
    )
    dao.insert(item: prefs) {err in
        if let err {
            print("Failed to insert into app db" , err)
        } else {
            print("success to db write")
        }
    }
}
