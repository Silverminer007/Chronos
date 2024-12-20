import React from "react";
import {Badge, Button, IconButton, SvgIcon, TextField, Typography} from "@mui/material";

type Date = {
    title: string,
    day: string,
    time: string,
    color: string,
    personStatus: "committed" | "cancelled" | "none",
    pollRunning: boolean
}

type Month = {
    dates: Date[],
    name: string
}

const months: Month[] = [
    {
        name: "Dezember",
        dates: [
            {
                title: "Teammeeting",
                day: "12",
                time: "19:00 Uhr",
                color: "#008800",
                personStatus: "committed",
                pollRunning: true
            },
            {
                title: "Teammeeting",
                day: "14",
                time: "19:00 Uhr",
                color: "#880088",
                personStatus: "none",
                pollRunning: false
            },
            {
                title: "Teammeeting",
                day: "15",
                time: "19:00 Uhr",
                color: "#440088",
                personStatus: "cancelled",
                pollRunning: false
            },
            {
                title: "Teammeeting",
                day: "18",
                time: "19:00 Uhr",
                color: "#008888",
                personStatus: "cancelled",
                pollRunning: true
            }
        ]
    },
    {
        name: "Januar",
        dates: [
            {
                title: "Teammeeting",
                day: "01",
                time: "19:00 Uhr",
                color: "#880000",
                personStatus: "none",
                pollRunning: true
            },
            {
                title: "Teammeeting",
                day: "12",
                time: "19:00 Uhr",
                color: "#888800",
                personStatus: "none",
                pollRunning: false
            },
            {
                title: "Teammeeting",
                day: "16",
                time: "19:00 Uhr",
                color: "#449944",
                personStatus: "cancelled",
                pollRunning: false
            },
            {
                title: "Teammeeting",
                day: "22",
                time: "19:00 Uhr",
                color: "#440088",
                personStatus: "cancelled",
                pollRunning: true
            }
        ]
    },
    {
        name: "Februar",
        dates: [
            {
                title: "Teammeeting",
                day: "01",
                time: "19:00 Uhr",
                color: "#880000",
                personStatus: "none",
                pollRunning: true
            },
            {
                title: "Teammeeting",
                day: "12",
                time: "19:00 Uhr",
                color: "#888800",
                personStatus: "none",
                pollRunning: false
            },
            {
                title: "Teammeeting",
                day: "16",
                time: "19:00 Uhr",
                color: "#449944",
                personStatus: "cancelled",
                pollRunning: false
            },
            {
                title: "Teammeeting",
                day: "22",
                time: "19:00 Uhr",
                color: "#440088",
                personStatus: "cancelled",
                pollRunning: true
            }
        ]
    },
    {
        name: "März",
        dates: [
            {
                title: "Teammeeting",
                day: "01",
                time: "19:00 Uhr",
                color: "#880000",
                personStatus: "none",
                pollRunning: true
            },
            {
                title: "Teammeeting",
                day: "12",
                time: "19:00 Uhr",
                color: "#888800",
                personStatus: "none",
                pollRunning: false
            },
            {
                title: "Teammeeting",
                day: "16",
                time: "19:00 Uhr",
                color: "#449944",
                personStatus: "cancelled",
                pollRunning: false
            },
            {
                title: "Teammeeting",
                day: "22",
                time: "19:00 Uhr",
                color: "#440088",
                personStatus: "cancelled",
                pollRunning: true
            }
        ]
    }
]

function Header({className}: { className?: string }) {
    const [search, setSearch] = React.useState<boolean>(false);
    return (
        <div
            className={"flex flex-row w-full items-center justify-between bg-blue-800 p-4 h-24 rounded-b-md " + className}>
            <h4 className="flex-grow">
                Termine 1 - 20
            </h4>
            <div className="flex flew-row items-center gap-2">
                <TextField className={" " + (search ? "block" : "hidden")}/>
                <button onClick={() => setSearch(!search)}>
                    <SvgIcon className="size-8">
                        <svg dataSlot="icon" aria-hidden="true" fill="none" strokeWidth={1.5} stroke="currentColor"
                             viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                            <path d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z"
                                  strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                    </SvgIcon>
                </button>
            </div>
        </div>
    );
}

function DateEntry({className, date}: { className?: string, date: Date }) {
    return (
        <div className={"flex flex-row gap-2 items-start " + className}>
            <div className="rounded-full p-1 mt-2 bg-blue-200 size-8 flex items-center justify-center">
                <Typography variant="h6" className="font-bold text-md text-black" align="center">
                    {date.day}
                </Typography>
            </div>
            <div className={"flex flex-row flex-grow items-center rounded-md gap-2 p-2"}
                 style={{backgroundColor: date.color}}>
                <div className="flex flex-row flex-grow items-center gap-2">
                    <Typography variant="body2">
                        {date.time}
                    </Typography>
                    <Typography variant="body1">
                        {date.title}
                    </Typography>
                </div>
                <IconButton
                    size="small"
                    className={"bg-blue-300 rounded-md " + (date.pollRunning || date.personStatus == "committed" ? "block" : "hidden")}>
                    <SvgIcon color={date.personStatus == "committed" ? "success" : "inherit"}>
                        <svg dataSlot="icon" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24"
                             xmlns="http://www.w3.org/2000/svg">
                            <path
                                d="M7.493 18.5c-.425 0-.82-.236-.975-.632A7.48 7.48 0 0 1 6 15.125c0-1.75.599-3.358 1.602-4.634.151-.192.373-.309.6-.397.473-.183.89-.514 1.212-.924a9.042 9.042 0 0 1 2.861-2.4c.723-.384 1.35-.956 1.653-1.715a4.498 4.498 0 0 0 .322-1.672V2.75A.75.75 0 0 1 15 2a2.25 2.25 0 0 1 2.25 2.25c0 1.152-.26 2.243-.723 3.218-.266.558.107 1.282.725 1.282h3.126c1.026 0 1.945.694 2.054 1.715.045.422.068.85.068 1.285a11.95 11.95 0 0 1-2.649 7.521c-.388.482-.987.729-1.605.729H14.23c-.483 0-.964-.078-1.423-.23l-3.114-1.04a4.501 4.501 0 0 0-1.423-.23h-.777ZM2.331 10.727a11.969 11.969 0 0 0-.831 4.398 12 12 0 0 0 .52 3.507C2.28 19.482 3.105 20 3.994 20H4.9c.445 0 .72-.498.523-.898a8.963 8.963 0 0 1-.924-3.977c0-1.708.476-3.305 1.302-4.666.245-.403-.028-.959-.5-.959H4.25c-.832 0-1.612.453-1.918 1.227Z"/>
                        </svg>
                    </SvgIcon>
                </IconButton>
                <IconButton
                    size="small"
                    className={"bg-blue-300 rounded-md " + (!date.pollRunning && date.personStatus == "none" ? "block" : "hidden")}>
                    <SvgIcon className="size-6" color="info">
                        <svg dataSlot="icon" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24"
                             xmlns="http://www.w3.org/2000/svg">
                            <path clipRule="evenodd"
                                  d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12Zm11.378-3.917c-.89-.777-2.366-.777-3.255 0a.75.75 0 0 1-.988-1.129c1.454-1.272 3.776-1.272 5.23 0 1.513 1.324 1.513 3.518 0 4.842a3.75 3.75 0 0 1-.837.552c-.676.328-1.028.774-1.028 1.152v.75a.75.75 0 0 1-1.5 0v-.75c0-1.279 1.06-2.107 1.875-2.502.182-.088.351-.199.503-.331.83-.727.83-1.857 0-2.584ZM12 18a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Z"
                                  fillRule="evenodd"/>
                        </svg>
                    </SvgIcon>
                </IconButton>
                <IconButton
                    size="small"
                    className={"bg-blue-300 rounded-md " + (date.pollRunning || date.personStatus == "cancelled" ? "block" : "hidden")}>
                    <SvgIcon className="size-6" color={date.personStatus == "cancelled" ? "error" : "inherit"}>
                        <svg dataSlot="icon" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24"
                             xmlns="http://www.w3.org/2000/svg">
                            <path
                                d="M15.73 5.5h1.035A7.465 7.465 0 0 1 18 9.625a7.465 7.465 0 0 1-1.235 4.125h-.148c-.806 0-1.534.446-2.031 1.08a9.04 9.04 0 0 1-2.861 2.4c-.723.384-1.35.956-1.653 1.715a4.499 4.499 0 0 0-.322 1.672v.633A.75.75 0 0 1 9 22a2.25 2.25 0 0 1-2.25-2.25c0-1.152.26-2.243.723-3.218.266-.558-.107-1.282-.725-1.282H3.622c-1.026 0-1.945-.694-2.054-1.715A12.137 12.137 0 0 1 1.5 12.25c0-2.848.992-5.464 2.649-7.521C4.537 4.247 5.136 4 5.754 4H9.77a4.5 4.5 0 0 1 1.423.23l3.114 1.04a4.5 4.5 0 0 0 1.423.23ZM21.669 14.023c.536-1.362.831-2.845.831-4.398 0-1.22-.182-2.398-.52-3.507-.26-.85-1.084-1.368-1.973-1.368H19.1c-.445 0-.72.498-.523.898.591 1.2.924 2.55.924 3.977a8.958 8.958 0 0 1-1.302 4.666c-.245.403.028.959.5.959h1.053c.832 0 1.612-.453 1.918-1.227Z"/>
                        </svg>
                    </SvgIcon>
                </IconButton>
            </div>
        </div>
    )
}

function Content({className}: { className?: string }) {
    return (
        <div className={"flex flex-col overflow-auto m-2 gap-2 " + className}>
            {
                months ?
                    months.map(month => {
                        return (
                            month.dates &&
                            <>
                                <Badge className="bg-blue-800 w-fit rounded-md px-1">
                                    {month.name}
                                </Badge>
                                {
                                    month.dates.map(date => <DateEntry date={date}/>)
                                }
                            </>
                        )
                    })
                    : <h6>Bisher keine Termine zu sehen</h6>
            }
        </div>
    )
}

function Footer({className}: { className?: string }) {
    return (
        <div className={"flex flex-row justify-between items-stretch m-2 " + className}>
            <IconButton className="p-2 bg-slate-400 rounded-md flex flex-row items-center justify-center text-white">
                <SvgIcon>
                    <svg dataSlot="icon" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24"
                         xmlns="http://www.w3.org/2000/svg">
                        <path clipRule="evenodd"
                              d="M11.03 3.97a.75.75 0 0 1 0 1.06l-6.22 6.22H21a.75.75 0 0 1 0 1.5H4.81l6.22 6.22a.75.75 0 1 1-1.06 1.06l-7.5-7.5a.75.75 0 0 1 0-1.06l7.5-7.5a.75.75 0 0 1 1.06 0Z"
                              fillRule="evenodd"/>
                    </svg>
                </SvgIcon>
            </IconButton>
            <Button className="p-2 bg-slate-400 rounded-md gap-1 flex flex-row items-center justify-center text-white">
                <SvgIcon>
                    <svg dataSlot="icon" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24"
                         xmlns="http://www.w3.org/2000/svg">
                        <path clipRule="evenodd"
                              d="M12 3.75a.75.75 0 0 1 .75.75v6.75h6.75a.75.75 0 0 1 0 1.5h-6.75v6.75a.75.75 0 0 1-1.5 0v-6.75H4.5a.75.75 0 0 1 0-1.5h6.75V4.5a.75.75 0 0 1 .75-.75Z"
                              fillRule="evenodd"/>
                    </svg>
                </SvgIcon>
                <Typography variant="body1">
                    Erstellen
                </Typography>
            </Button>
            <IconButton className="p-2 bg-slate-400 rounded-md flex flex-row items-center justify-center text-white">
                <SvgIcon>
                    <svg dataSlot="icon" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24"
                         xmlns="http://www.w3.org/2000/svg">
                        <path clipRule="evenodd"
                              d="M12.97 3.97a.75.75 0 0 1 1.06 0l7.5 7.5a.75.75 0 0 1 0 1.06l-7.5 7.5a.75.75 0 1 1-1.06-1.06l6.22-6.22H3a.75.75 0 0 1 0-1.5h16.19l-6.22-6.22a.75.75 0 0 1 0-1.06Z"
                              fillRule="evenodd"/>
                    </svg>
                </SvgIcon>
            </IconButton>
        </div>
    )
}

export default function Calendar() {

    return (
        <div className="flex flex-col h-screen w-screen">
            <Header/>
            <Content className="flex-grow"/>
            <Footer/>
        </div>
    );
}